# harvestr-data-archive
Retrieve Harvestr data available in APIs and save them in JSON and into an XLSX sheet

This is a script-like, use one time and throw away program in Scala, but it demoed enough interesting things to share it, hoping it can be useful to others. 

And perhaps you, too, need to do some archiving of harvester data, or want to be able to manipulate them in way that Harvestr doesn't let you do. 

So, basically that code shows how going from HTTP Rest to XLSX, keeping JSON in the middle.


- 1/ simple HTTP client with nice [Li Haoyi's request library](https://github.com/com-lihaoyi/requests-scala). Just work, no question, ideal for that kind of throw away code

- 2/ JSON parsing with [ZIO Json](https://zio.dev/zio-json/). It works very well for mapping to scala objects, and aliases are perfect for the kind of JS-or-Python oriented JSON used by Harvestr

- 3/ creating an XLSX spreadsheet with [Apache POI](https://poi.apache.org/). It seems to be the universal exchange media among !dev, and with Google Sheet it's now an archiving media. That's... Erk. Fractally broken. But well at least you can see how to do that kind of dirty work here. 

- 4/ also showing nice little lib in the middle, like [Better files](https://github.com/pathikrit/better-files) which is such a nice File manipulation lib in Scala, or [flexmark-java](https://github.com/vsch/flexmark-java/) that is able to convert HTML to markdown and the opposite too (and under the hood, the fabulous [JSoup](https://jsoup.org/), with the best name ever for what it does). 

