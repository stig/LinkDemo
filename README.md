Pagination
==========

A demo of three different ways to do pagination using the HTTP header as
described in http://www.rfc-editor.org/rfc/rfc5988.txt using Spray Routing.
Using an offset + limit, or page + page_size parameters isn't always
appropriate. It's inspired by GitHub's API v3 pagination:
http://developer.github.com/v3/#pagination

By always relying on the next/prev/last/first rels different API endpoints can
use different types of arguments for pagination, yet they can still be treated
the same by clients. Because they don't care what the URL or the arguments are;
just that it can find the URL in the  header. As such it is important not to try
to build your own links for pagination but follow the links given to you.

This improves our ability to cache the requests for subsequent pages, since
there's less room for clients to assemble URIs differently.

Cache-Control
=============

For good measure this demo also shows how you can add a Cache-Control header
with a max-age based on the result set. This example is fairly contrived; we
just cache each returned slice for the lowest time of any item in the slice.

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
