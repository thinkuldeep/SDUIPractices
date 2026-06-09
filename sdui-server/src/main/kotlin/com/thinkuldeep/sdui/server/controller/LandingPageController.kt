package com.thinkuldeep.sdui.server.controller

import com.thinkuldeep.sdui.server.model.Button
import com.thinkuldeep.sdui.server.model.Column
import com.thinkuldeep.sdui.server.model.FeaturedItems
import com.thinkuldeep.sdui.server.model.Image
import com.thinkuldeep.sdui.server.model.Row
import com.thinkuldeep.sdui.server.model.Text
import com.thinkuldeep.sdui.server.model.UiComponent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class LandingPageController {

    @GetMapping("/api/ui/landing")
    fun landingPage(): UiComponent {
        val title = Text( value = "Welcome to Kuldeep's Space", size = "large", weight = "bold")

        val subtitle = Text( value = "The world of learning, sharing, and caring",  size = "medium", weight = "medium")

        val books = FeaturedItems( button=Button(id= "books", value = "Books - Click for more!", action = "load_next_feature"), children = listOf(
                Column(children = listOf(Text( value = "Jagjeevan - Living Larger Than Life",  size = "medium", weight = "bold"), Image( url = "https://thinkuldeep.com/images/Jagjeevan_book.png", width = 488, height = 503))),
                Image(url = "https://thinkuldeep.com/images/exploring-the-metaverse-books.png", width = 465, height = 503),
                Image(url = "https://thinkuldeep.com/images/MyThoughtworkings.jpg", width = 893, height = 503),
            )
        )

        val articles = FeaturedItems(button=Button(id= "articles", value = "Articles - Click for more!", action = "load_next_feature"), children = listOf(
            Column(children = listOf(Text( value = "Connecting Thread Devices to the Internet over CoAP",  size = "medium", weight = "bold"),
                Text( value = "A step-by-step guide to implementing COAP server and COAP client Thread network devices",  size = "medium", weight = "medium"))),
            Column(children = listOf(Text( value = "Jagjeevan at Silver Jubilee of Millennium Batch NIT Kurukshetra",  size = "medium", weight = "bold"),
                Text( value = "Got a Dose of Jagjeevan at Silver Jubilee of Millennium Batch NIT Kurukshetra!",  size = "medium", weight = "medium")))
            )
        )

        val footer = Text(value = "We learn every day in all situations of life, good or bad. The learning matures only by sharing, you explore more of yourself by giving it away to others. Writing helps me discover what I know, and that I can say confidently. I created this space to share my learning with great care.",  size = "medium",  weight = "medium");

        return Column( children = listOf( title,  subtitle, books, articles, footer))
    }
}