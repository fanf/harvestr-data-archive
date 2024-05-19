package fanf

import com.vladsch.flexmark.html2md.converter.*
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.*
import java.time.ZonedDateTime
import java.util
import org.jsoup.nodes.Element
import zio.*
import zio.json.*

object Data {

  final case class Description(value: String) {

    override def toString: String = {
      Description.converter.convert(value)
    }
  }

  object Description {
    // ffs java...
    // all the following parts are there just to be able to output <<<image deleted>>> in place of base64 image
    // because they are too big for xlx cells.
    private object IgnoreImage extends HtmlNodeRenderer {
      override def getHtmlNodeRendererHandlers: util.Set[HtmlNodeRendererHandler[?]] = {
        new util.HashSet[HtmlNodeRendererHandler[?]](
          java.util.Collections.singletonList(
            new HtmlNodeRendererHandler[Element](
              "img",
              classOf[Element],
              processImg _
            )
          )
        )
      }

      private def processImg(node: Element, context: HtmlNodeConverterContext, out: HtmlMarkdownWriter): Unit = {
        out.append("<<<image deleted>>>")
      }

      object Factory extends HtmlNodeRendererFactory {
        override def apply(options: DataHolder): HtmlNodeRenderer = {
          IgnoreImage
        }
      }
    }

    private object Extension extends FlexmarkHtmlConverter.HtmlConverterExtension {
      override def rendererOptions(options: MutableDataHolder): Unit = {}

      override def extend(builder: FlexmarkHtmlConverter.Builder): Unit = {
        builder.htmlNodeRendererFactory(IgnoreImage.Factory)
      }
    }

    private val options: MutableDataSet        = MutableDataSet().set(
      Parser.EXTENSIONS,
      java.util.Collections.singletonList(Extension)
    )
    val converter:       FlexmarkHtmlConverter = FlexmarkHtmlConverter.builder(options).build

    implicit val codecDescription: JsonCodec[Description] = JsonCodec.string.transform[Description](
      Description.apply,
      _.toString
    )
  }

  final case class PageInfo(next_offset: Int, per_page: Int)

  object PageInfo {
    implicit val codecPageInfo: JsonCodec[PageInfo] = DeriveJsonCodec.gen
  }

  final case class Discovery(
      id:               String,
      clientId:         String,
      createdAt:        ZonedDateTime,
      updatedAt:        ZonedDateTime,
      title:            String,
      description:      Option[Description],
      discoveryStateId: String,
      parentId:         String,
      parentType:       String,
      assigneeId:       Option[String],
      tags:             Chunk[String]
  )

  object Discovery {
    implicit val codecDiscoverySummary: JsonCodec[Discovery] = DeriveJsonCodec.gen
  }

  final case class Selection(
      id:            String,
      clientId:      String,
      createdAt:     ZonedDateTime,
      updatedAt:     ZonedDateTime,
      content:       String,
      fullSelection: Boolean
  )

  object Selection {
    implicit val codecSelection: JsonCodec[Selection] = DeriveJsonCodec.gen
  }

  final case class Feedback(
      id:          String,
      clientId:    String,
      createdAt:   ZonedDateTime,
      updatedAt:   ZonedDateTime,
      starred:     Boolean,
      score:       Int,
      messageId:   String,
      discoveryId: String,
      selections:  Chunk[Selection]
  )

  object Feedback {
    implicit val codecFeedback: JsonCodec[Feedback] = DeriveJsonCodec.gen
  }

  final case class Message(
      id:             String,
      clientId:       String,
      receivedAt:     Option[ZonedDateTime],
      createdAt:      ZonedDateTime,
      updatedAt:      ZonedDateTime,
      integrationUrl: Option[String],
      integrationId:  Option[String],
      title:          String,
      content:        String,
      channel:        String,
      read:           Boolean,
      updated:        Boolean,
      archived:       Boolean,
      bin:            Boolean,
      assigneeId:     Option[String],
      requesterId:    String,
      submitterId:    String,
      labels:         Chunk[String]
  )

  object Message {
    implicit val codecMessage: JsonCodec[Message] = DeriveJsonCodec.gen
  }

  final case class Segment(
      id:        String,
      clientId:  String,
      name:      String,
      createdAt: ZonedDateTime,
      updatedAt: ZonedDateTime
  )

  object Segment {
    implicit val codecSegment: JsonCodec[Segment] = DeriveJsonCodec.gen
  }

  final case class User(
      id:          String,
      clientId:    String,
      createdAt:   ZonedDateTime,
      updatedAt:   ZonedDateTime,
      name:        String,
      email:       Option[String],
      phone:       Option[String],
      companyId:   Option[String],
      importId:    Option[String],
      externalUid: Option[String],
      segments:    Chunk[Segment]
  )

  object User {
    implicit val codecUser: JsonCodec[User] = DeriveJsonCodec.gen
  }

  final case class Company(
      id:          String,
      clientId:    String,
      name:        String,
      createdAt:   ZonedDateTime,
      updatedAt:   ZonedDateTime,
      importId:    Option[String],
      externalUid: Option[String],
      segments:    Chunk[Segment]
  )

  object Company {
    implicit val codecCompany: JsonCodec[Company] = DeriveJsonCodec.gen
  }

  final case class Component(
      id:          String,
      clientId:    String,
      createdAt:   ZonedDateTime,
      updatedAt:   ZonedDateTime,
      title:       String,
      description: Option[Description],
      parentId:    Option[String]
  )

  object Component {
    implicit val codecComponent: JsonCodec[Component] = DeriveJsonCodec.gen
  }

  final case class DiscoveryState(
      id:        String,
      clientId:  String,
      name:      String,
      createdAt: ZonedDateTime,
      updatedAt: ZonedDateTime
  )

  object DiscoveryState {
    implicit val codecDiscoveryState: JsonCodec[DiscoveryState] = DeriveJsonCodec.gen
  }

  final case class HarvestrResponse[A](
      code:          Int,
      messageStatus: String,
      pageInfos:     PageInfo,
      @jsonAliases("discoveries", "messages", "feedbacks", "users", "components", "companies", "discoveryStates")
      elements:      Chunk[A]
  )

  object HarvestrResponse {
    implicit def codecDiscoveries[A](implicit codec: JsonCodec[A]): JsonCodec[HarvestrResponse[A]] = DeriveJsonCodec.gen
  }
}
