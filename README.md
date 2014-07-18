# website

A Clojure library designed to ... well, that part is up to you.

## Usage

FIXME

## Database structure

- Pages
  - Name
  - MD text
- Menu
  - Ordered list of pages
- Blog
  - Posts
    - Title
    - MD text
- Updates
  - Posts
    - Title
    - MD text

## Setup MongoDB

mongo

In mongo command line enter

db = db.getSiblingDB('admin')

db.addUser({user:"root",pwd:"password",roles:["userAdminAnyDatabase", "readWrite"]})

db.addUser({user:"probee",pwd:"password", roles:["readWrite"]})

In terminal enter

sudo vim /etc/mongodb.conf

Uncomment the line

    auth = true

service mongodb restart

Login as admin

mongo <server_ip>/admin -u root -p <password>

Login as user

mongo <server_ip>/project -u probee -p <password>

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
