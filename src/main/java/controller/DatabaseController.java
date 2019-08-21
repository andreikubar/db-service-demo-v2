package controller;

import model.Database;
import model.NoDataFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/database")
public class DatabaseController {

    private Database databaseService;

    public DatabaseController(Database databaseService) {
        this.databaseService = databaseService;
    }

    @GetMapping("/{table}/{id}")
    public ResponseEntity<Object> selectRow(@PathVariable String table, @PathVariable Integer id){
        List<String> queryResult;
        try {
            queryResult = databaseService.select(table, id);
        } catch (NoDataFoundException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No data found");
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
        return ResponseEntity.ok().body(queryResult);
    }

    @PostMapping("/{table}")
    public ResponseEntity<Integer> insertRow(@PathVariable String table, @RequestBody List<String> values){
        int insert = -1 ;
        try {
            insert = databaseService.insert(table, values);
        } catch (Exception e){
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
        return ResponseEntity.ok(insert);
    }

    @PutMapping("/{table}/{id}")
    public Boolean updateRow(@PathVariable String table, @PathVariable Integer id, @RequestBody List<String> values){
        return databaseService.update(table, values, id);
    }
}
