package com.qingqiu.openagent.agent.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class DataBaseTool implements ITool {

    private final JdbcTemplate jdbcTemplate;

    public DataBaseTool(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String getName() {
        return "dataBaseTool";
    }

    @Override
    public String getDescription() {
        return "Read-only SQL query tool for PostgreSQL";
    }

    @Override
    public ToolType getType() {
        return ToolType.OPTIONAL;
    }

    @Tool(name = "databaseQuery", value = "Execute a read-only SQL SELECT query against PostgreSQL and return formatted rows")
    public String query(
            @P(value = "Read-only SELECT SQL statement. Must start with SELECT; INSERT/UPDATE/DELETE/DDL are rejected.")
            String sql) {
        try {
            String trimmedSql = sql == null ? "" : sql.trim().toUpperCase();
            if (!trimmedSql.startsWith("SELECT")) {
                return "Error: only SELECT is allowed.";
            }

            List<String> rows = jdbcTemplate.query(sql, (ResultSet rs) -> {
                List<String> resultRows = new ArrayList<>();
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                List<String> columnNames = new ArrayList<>();
                List<Integer> columnWidths = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    columnNames.add(columnName);
                    columnWidths.add(columnName.length());
                }

                List<List<String>> dataRows = new ArrayList<>();
                while (rs.next()) {
                    List<String> rowData = new ArrayList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        Object value = rs.getObject(i);
                        String valueStr = value == null ? "NULL" : value.toString();
                        rowData.add(valueStr);
                        columnWidths.set(i - 1, Math.max(columnWidths.get(i - 1), valueStr.length()));
                    }
                    dataRows.add(rowData);
                }

                StringBuilder header = new StringBuilder("| ");
                for (int i = 0; i < columnCount; i++) {
                    header.append(String.format("%-" + columnWidths.get(i) + "s", columnNames.get(i))).append(" | ");
                }
                resultRows.add(header.toString());

                StringBuilder separator = new StringBuilder("|");
                for (int i = 0; i < columnCount; i++) {
                    separator.append("-".repeat(columnWidths.get(i) + 2)).append("|");
                }
                resultRows.add(separator.toString());

                if (dataRows.isEmpty()) {
                    resultRows.add("| (no data) |");
                } else {
                    for (List<String> rowData : dataRows) {
                        StringBuilder row = new StringBuilder("| ");
                        for (int i = 0; i < columnCount; i++) {
                            row.append(String.format("%-" + columnWidths.get(i) + "s", rowData.get(i))).append(" | ");
                        }
                        resultRows.add(row.toString());
                    }
                }
                return resultRows;
            });

            return "Query result:\n" + String.join("\n", rows);
        } catch (Exception e) {
            log.error("SQL tool error", e);
            return "Error: " + e.getMessage();
        }
    }
}
