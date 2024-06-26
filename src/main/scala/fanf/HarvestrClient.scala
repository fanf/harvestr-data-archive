package fanf

import better.files.*
import java.time.ZonedDateTime
import java.util
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import requests.*
import zio.*
import zio.json.*

/**
 * This file allows to read all you Harvestr data, save them to JSON files, and
 * once you have them in JSON, put everything into an XLSX file. It can be handy to
 * make local archiving.
 *
 * Harvestr description contain horrible HTML. This is replaced by more meaningful markdown.
 */

object Harvestr {

  val baseDir: File = File("/tmp/harvestr")

  val jsonDir: File = baseDir / "export_json"

  jsonDir.createDirectories()

  // you Harvestr token, which must be able to read all data
  val token: String = (baseDir / "token_readonly_archive").contentAsString.strip()

  // see https://developers.harvestr.io/api
  def url(element: String) = s"https://rest.harvestr.io/v1/${element}"

}



object Fail {
  def apply(msg: String = ""): Nothing = throw new RuntimeException(s"Failed by dev exception: ${msg}")
}

/*
 * The rest client in charge of doing HTTP  request and transform resulting JSON into objects.
 * You could directly do the request by hand and avoid the parsing of inner objects, but:
 * - since requests only return max 100 elements, you have to have a way to query following ones
 * - the parsing allowed to check that the JSON objects were correctly understood.
 * Still, if you have a lot of items, perhaps you should avoid read json/parse array of elems/convert back elems to json
 *
 * Once all json are stored, we read them back to build the XLSX, one tab for each element type.
 * There is some cleaning done here: description are transformed to Markdown, base64 images removed.
 *
 * Again, we could step directly from HTTP request to parsed elements to writing XLSX, but keeping the JSON
 * as an interest by itself if you want archiving. You will still be able to parse them back and do
 * whatever computing you need with the raw data.
 */
object HarvestrClient {
  import Data.*

  private def storeElements[A](offset: Int, dirName: String, elements: Chunk[A])(implicit jsonCodec: JsonCodec[A]): Unit = {
    val filename = s"${dirName}_${offset}.json"
    val json     = elements.toJsonPretty
    val dir      = (Harvestr.jsonDir / dirName).createDirectories()

    (dir / filename).writeText(json)
  }

  private def elementsReq(name: String, offset: Int): Response = {
    requests.get(
      Harvestr.url(name),
      headers = Map(("Accept", "application/json"), ("X-Harvestr-Private-App-Token", Harvestr.token)),
      params = Map(("per_page", "100"), ("offset", offset.toString))
    )
  }

  private def requestOneType[A](urlName: String, resName: String)(implicit codec: JsonCodec[A]): Unit = {
    println(s"Doing request for ${urlName}")
    var nextOffset = 0
    while (nextOffset >= 0) {
      println(s"Doing request '${urlName}' with next offset: ${nextOffset}")
      val response: Response = elementsReq(urlName, nextOffset)

      val text     = response.text()
      // if you have any doubt about what is returned
      // println(text)
      val elements = text.fromJson[HarvestrResponse[A]] match {
        case Left(value)  => Fail(value)
        case Right(value) => value
      }

      println(s"Next offset: ${elements.pageInfos.next_offset}")
      storeElements(nextOffset, resName, elements.elements)

      nextOffset = elements.pageInfos.next_offset
    }

  }

  /*
   * Write an excel at path, with one header line, one line for each element.
   * The element name is the same have the directory in export
   */
  def writeXlsx[A <: Product](baseDir: File, workBook: XSSFWorkbook, elementName: String)(implicit encoder: JsonCodec[A]) = {

    println("*****")
    // read things

    val files = (baseDir / elementName).children

    val elts = Chunk.fromIterator(files).flatMap { f =>
      f.contentAsString.fromJson[Chunk[A]] match {
        case Left(value)  => Fail(value)
        case Right(value) => value
      }
    }

    val size    = elts(0).productArity
    val headers = (0 until size).map(i => elts(0).productElementName(i))

    println(s"Writing result to file: => ${elementName}")

    val sheet   = workBook.createSheet(elementName)
    val drawing = sheet.createDrawingPatriarch()

    // create headers
    val headerRow = sheet.createRow(0)

    headers.zipWithIndex.foreach {
      case (h, i) =>
        headerRow.createCell(i).setCellValue(h)
    }

    // this is a limit of cell content. If you have description with more
    // text than that, you will have to find a strategy for splitting among several cells.
    val MAX_SIZE_TEXT = 32767

    // create all lines
    elts.zipWithIndex.foreach {
      case (p, i) =>
        val n          = (i + 1)
        val currentRow = sheet.createRow(n) // because row 0 is for headers
//        val nextRow    = sheet.createRow(n + 1)
        headers.zipWithIndex.foreach {
          case (_, j) =>
            val cell = currentRow.createCell(j)
            val text = p.productElement(j) match {
              // some quick & dirty clean-up
              case Some(x) => x.toString
              case None    => ""
              case Chunk() => ""
              case x       => x.toString
            }
            val t1   = text.take(MAX_SIZE_TEXT)
            cell.setCellValue(t1)
//            val c2   = nextRow.createCell(j)
//            c2.setCellValue(text.drop(MAX_SIZE_TEXT).take(MAX_SIZE_TEXT))
            if (text.size > MAX_SIZE_TEXT) {
              // so that you can see interactively that something is not working
              println(s"The size of text is too big: \n ${text}")
            }
        }
    }
    // resize if needed
    (0 until headers.size).foreach(i => sheet.autoSizeColumn(i))

    println("***** done *****")

  }

  def main(args: Array[String]): Unit = {

    /// request parts /// need an active Harvestr account ///

    if (true) {
      // download each set of elements and save the resulting json in
      // /tmp/harvestr/[users, companies, etc]/[users_0.json, users_100.json, etc]
      // Where each of the elements_N00.json file is a an array of the 100 of the elements
      requestOneType[User]("user", "users")
      requestOneType[Company]("company", "companies")
      requestOneType[Component]("component", "components")
      requestOneType[DiscoveryState]("discovery-state", "discoveryStates")
      requestOneType[Discovery]("discovery", "discoveries")
      requestOneType[Feedback]("feedback", "feedbacks")
      requestOneType[Message]("message", "messages")
    }

    // writing an XLSX in place of JSON
    // Getting XSSFWorkbook or HSSFWorkbook object based on excel file format
    val dest     = Harvestr.baseDir / "harvestr.xlsx"
    dest.delete(swallowIOExceptions = true)
    val workBook = new XSSFWorkbook

    /*
     * - we remove image, but they are not lost, they are base-64 encoded in the json
     * - id between elements are not resolved, it could be done with a two steps
     *   process where one first loads discovery states, etc ; then resolve all id
     *   with names or perhaps have object with more interesting data, then write.
     */

    writeXlsx[User](Harvestr.jsonDir, workBook, "users")
    writeXlsx[Company](Harvestr.jsonDir, workBook, "companies")
    writeXlsx[DiscoveryState](Harvestr.jsonDir, workBook, "discoveryStates")
    writeXlsx[Component](Harvestr.jsonDir, workBook, "components")
    writeXlsx[Discovery](Harvestr.jsonDir, workBook, "discoveries")
    writeXlsx[Feedback](Harvestr.jsonDir, workBook, "feedbacks")
    writeXlsx[Message](Harvestr.jsonDir, workBook, "messages")

    // finally write the XLSX file.
    dest.outputStream.foreach(workBook.write)

  }

}
