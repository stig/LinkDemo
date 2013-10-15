Pagination
==========

This is first and foremost a; demo of three different ways to do pagination
using the HTTP Link header as described in [RFC
5988](http://www.rfc-editor.org/rfc/rfc5988.txt), using Spray Routing. It's
inspired by how [GitHub's API pagination](
http://developer.github.com/v3/#pagination) works.

Using offset + limit (or page + page_size) parameters isn't always appropriate,
but it's awkward to force clients to handle different ways to build URLs for the
"next page" for every collection.

By always relying on the next/prev/last/first rels different API endpoints can
use different types of arguments for pagination, yet they can still be treated
the same by clients. (Because they don't care what the URL or the arguments are,
just that it can find the URL in the Link header.) As such it is important not
to try to build your own links for pagination but follow the links given to you.

One nice side effect is that this improves our ability to cache the results for
subsequent pages, since there's less room for clients to assemble URIs
differently.

Caching
=======

Under high load it is often better to serve slightly stale data than to buckle
under the load and serve no data at all. For the sake of completeness this demo
takes a two-pronged approach to caching, but it is important to note that both
approaches might not be appropriate for your app.

To truly make your API scalable, set the `Cache-Control` header's `max-age` to a
suitable value for each entity you return. This allows us fine-grained control
over how long downstream caches our result. This means we can tell downstream
that for resource A and E, cache them for 5 minutes but cache B, C and D for 10
minutes. With a cache immediately in front of our API, and possibly also at the
edges (such as via a CDN) we can scale GET requests to many, many requests per
second. This example is fairly contrived; we just cache each returned slice for
the lowest time of any item in the slice.

What happens when the max-age of a frequently-requested entity expires? One
possibility is that we suddenly get a lot of identical requests through to our
API, leading to it being overwhelmed. This is known as the *Thundering Herd*
problem. Here we attempt to deal with that by internally cache the *N* most
recent unique requests for a few seconds.

It is important to note that this simpleCache comes at a cost, and needs to be
tuned well. If you cache at most 10 values, but accept 11 unique requests before
the core has responded to the first you will simply make your API *slower*
because you have the overhead of checking the cache but never getting a hit.

By the way, Spray's caching mitigates the thundering herd problem by creating a
future for the first request, and if a second identical request comes in within
the TTL (time to live) *whether or not the core has already responded* it is
simply handed the same future. When the future is resolved, both clients get the
result of the future.


Expiring / Deleting items
=========================

If, when each item's cache timeout fired they were to expire, and be removed
from the list, we would have to cache each page by the minimum *of the entire
result set* rather than just the slice, because the pagination would be screwed
up.

In this case the by-offset, and by-id examples would break in different ways -
while the by-date example would still work.

* by-offset would appear to miss items, because if an item in the first page was
removed before we got the second page the first item in the previous second page
would have moved to the first!
* If the item whose id is used for pagination was removed, by-id could break in
a myriad of different ways, depending on implementation.

Adding items
============

All three pagination methods could break in interesting ways if you allow adding
to the middle of your data set. What's *middle*? Well, it depends on your sort
order...
:-)
