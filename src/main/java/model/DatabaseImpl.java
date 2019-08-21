package model;

import com.google.common.collect.Lists;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DatabaseImpl implements Database {

    private static Map<String, ReentrantReadWriteLock> tableLocks = new ConcurrentHashMap<>();
    public static final String dataStoreBaseDir = System.getProperty("user.home");
    private static Map<String, Integer> tableSequences = new HashMap<>();

    @Override
    public int insert(String tableName, List<String> values) {
        tableLocks.putIfAbsent(tableName, new ReentrantReadWriteLock());
        Lock tableWriteLock = tableLocks.get(tableName).writeLock();
        tableWriteLock.lock();
        Integer newId;
        try {
            newId = incrementId(tableName);
            List<String> valuesToInsert = Lists.newArrayList(values);
            valuesToInsert.add(0, String.valueOf(newId.intValue()));
            Path tableFile = Paths.get(dataStoreBaseDir, tableName);
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(tableFile.toFile(), true))) {
                bw.write(String.join(",", valuesToInsert));
                bw.newLine();
            } catch (IOException e) {
                System.out.println("Failed to write file " + tableFile);
                e.printStackTrace();
            }
        } finally {
            tableWriteLock.unlock();
        }
        return newId;
    }

    @Override
    public boolean update(String tableName, List<String> values, int id) {
        Lock tableWriteLock = Optional.ofNullable(tableLocks.get(tableName))
                .orElseThrow(TableNotFoundException::new)
                .writeLock();
        tableWriteLock.lock();
        try {
            Path tableFile = Paths.get(dataStoreBaseDir, tableName);
            List<String> valuesToInsert = Lists.newArrayList(values);
            valuesToInsert.add(0, String.valueOf(id));
            StringBuilder outputBuffer = new StringBuilder();

            try (BufferedReader file = new BufferedReader(new FileReader(tableFile.toFile()))) {
                String line;
                while ((line = file.readLine()) != null) {
                    if (line.startsWith(String.valueOf(id))) {
                        outputBuffer.append(String.join(",", valuesToInsert));
                    } else {
                        outputBuffer.append(line);
                    }
                    outputBuffer.append("\n");
                }
            } catch (IOException e) {
                System.out.println("Failed to read file " + tableFile);
                e.printStackTrace();
                return false;
            }

            try (FileOutputStream out = new FileOutputStream(tableFile.toFile())) {
                out.write(outputBuffer.toString().getBytes());
            } catch (IOException e) {
                System.out.println("Failed to write file " + tableFile);
                e.printStackTrace();
                return false;
            }
        } finally {
            tableWriteLock.unlock();
        }

        return true;
    }

    @Override
    public List<String> select(String tableName, int id) {
        tableLocks.putIfAbsent(tableName, new ReentrantReadWriteLock());
        Lock tableReadLock = tableLocks.get(tableName).readLock();
        tableReadLock.lock();
        List<String> ret = null;
        try (Stream<String> stream = Files.lines(Paths.get(dataStoreBaseDir, tableName))) {
            ret = stream
                    .filter(x -> x.startsWith(String.valueOf(id)))
                    .map(x -> x.split(","))
                    .flatMap(Stream::of)
                    .skip(1)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            tableReadLock.unlock();
        }

        if (ret == null || ret.size() == 0) {
            throw new NoDataFoundException();
        }

        return ret;
    }

    private Integer incrementId(String tableName) {
        tableSequences.putIfAbsent(tableName, 0);
        Integer newId = tableSequences.get(tableName) + 1;
        tableSequences.put(tableName, newId);
        return newId;
    }
}
