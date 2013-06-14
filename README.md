# feedletter

Send Atom or RSS feed updates via plain-text email (one per feed). Optionally, feed items can be archived (HTML file only).

## Usage

### Configure

    {
     :from "feedletter@example.com"
     :to "me@example.com"
     :feeds ["http://www.xkcd.com/atom.xml"
             "http://www.newyorker.com/online/blogs/elements/rss.xml"
             "http://bits.blogs.nytimes.com/feed/"
             "http://daringfireball.net/index.xml"]
     }

### Run

    java -jar feedletter.jar config.edn

### Reset

    rm -rf .state
    
## Sample feedletter

    Date: Mon, 13 May 2013 13:11:41 +0200 (CEST)
    From: feedletter@example.com
    To: me@example.com
    Subject: Feed update: Elements -- 6 new items

    Terrible News About Carbon and Climate Change
    http://www.newyorker.com/online/blogs/elements/2013/05/terrible-news-about-carbon-and-climate-change.html

    The Evolution of the Web, in a Blink
    http://www.newyorker.com/online/blogs/elements/2013/05/the-evolution-of-the-web-in-a-blink.html

    From the Twitter Feed of David Grann: The Brightest Explosion Ever Seen and Fifteen-Thousand-Year-Old Words
    http://www.newyorker.com/online/blogs/elements/2013/05/from-the-twitter-feed-of-david-grann-the-brightest-explosion-ever-seen.html

    Building the Future, One Crazy Idea at a Time
    http://www.newyorker.com/online/blogs/elements/2013/05/5d-science-of-fiction-conference-futurist-ideas.html

    An Exploding Star, a Grain of Sand, and an Origin Story
    http://www.newyorker.com/online/blogs/elements/2013/05/an-exploding-star-a-grain-of-sand-and-an-origin-story.html

    The Song of the Cicada
    http://www.newyorker.com/online/blogs/elements/2013/05/the-song-of-the-cicada.html
