# DB service demo

## Notes
Datafiles will be written into the "user.home" directory. Each file will be called using the table name.
Make sure old files are deleted manually before starting the application.


## Inserting
`curl -X POST "http://localhost:8080/database/table1" -d '["val1","val2"]' --header "Content-Type:application/json"`
 
## Selecting
`curl "http://localhost:8080/database/table1/1"`

## Updating
`curl -X PUT "http://localhost:8080/database/table1/1" -d '["newval1","newval2"]' --header "Content-Type:application/json"`
