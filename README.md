A demo of different ways to do pagination using the HTTP  header as described in
http://www.rfc-editor.org/rfc/rfc5988.txt using Spray Routing.

By always relying on the next/prev/last/first rel s different API endpoints can
use different types of arguments for pagination, yet can still be treated the
same by clients. (Because they don't care what the URL or the arguments are;
just that it can find the URL in the  header.)

This isn't my own invention, by the way. It's inspired by GitHub's API
pagination works: http://developer.github.com/v3/#pagination As noted there it
is important not to try to build your own links for pagination but follow the
links they give you.
