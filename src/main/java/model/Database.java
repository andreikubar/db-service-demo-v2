package model;

import java.util.List;

public interface Database {
    // returns the id of the newly inserted row or throws an exception in case of error
    int insert(String tableName, List<String> values);

    // updates the data associated with the list
    // returns true if the data has changed, or throws an exception in case of error
    boolean update(String tableName, List<String> values, int id);

    // retrieves the data associated to the id (as previously stored/updated),
    // or throws an exception in case of error or missing data
    List<String> select(String tableName, int id);
}
