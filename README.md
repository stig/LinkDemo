A demo of different ways to do pagination using the HTTP  header
as described in http://www.rfc-editor.org/rfc/rfc5988.txt using Spray Routing.

By always relying on the next/prev/last/first rel s different API endpoints
can use different types of arguments for pagination, yet can still be treated
the same by clients. (Because they don't care what the URL or the arguments are; just that it can find the URL in the  header.)
