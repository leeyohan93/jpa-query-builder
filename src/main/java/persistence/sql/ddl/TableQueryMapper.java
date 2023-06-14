package persistence.sql.ddl;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TableQueryMapper {
    private static final String SPACE = " ";
    private static final String CREATE_QUERY_FORMAT = "create table %s";
    private static final String DROP_QUERY_FORMAT = "drop table %s";
    private static final String ID_QUERY_FORMAT = "%s primary key";
    private static final String COLUMN_QUERY_FORMAT = "(%s, %s)";
    private static final String COLUMN_SEPARATOR = ", ";
    private final ColumnMapper columnMapper;
    private final GeneratedValueStrategy generatedValueStrategy;

    public TableQueryMapper(ColumnMapper columnMapper, GeneratedValueStrategy generatedValueStrategy) {
        this.columnMapper = columnMapper;
        this.generatedValueStrategy = generatedValueStrategy;
    }

    public String create(Class<?> clazz) {
        String tableQuery = tableQuery(clazz);
        String columnsQuery = columnsQuery(clazz);

        return String.format("%s %s", tableQuery, columnsQuery);
    }

    private String tableQuery(Class<?> clazz) {
        return String.format(CREATE_QUERY_FORMAT, getTableName(clazz));
    }

    private String getTableName(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);

        if (table != null && !table.name().isBlank()) {
            return table.name();
        }

        return clazz.getSimpleName();
    }

    private String columnsQuery(Class<?> clazz) {
        Field[] declaredFields = clazz.getDeclaredFields();
        String idColumnQuery = getIdColumnQuery(declaredFields);
        String columnsQuery = getColumnsQuery(declaredFields);

        return String.format(COLUMN_QUERY_FORMAT, idColumnQuery, columnsQuery);
    }

    private String getIdColumnQuery(Field[] declaredFields) {
        Field idField = Arrays.stream(declaredFields)
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("@Id 어노테이션이 선언된 필드가 존재하지 않습니다."));

        StringBuilder queryBuilder = new StringBuilder(ID_QUERY_FORMAT);
        String generate = generatedValueStrategy.generate(idField.getAnnotation(GeneratedValue.class));

        if (!generate.isBlank()) {
            queryBuilder.append(SPACE).append(generate);
        }

        return String.format(queryBuilder.toString(), columnMapper.column(idField));
    }

    private String getColumnsQuery(Field[] declaredFields) {
        List<Field> fields = Arrays.stream(declaredFields)
                .filter(field -> !field.isAnnotationPresent(Id.class))
                .filter(filed -> !filed.isAnnotationPresent(Transient.class))
                .collect(Collectors.toList());

        return fields.stream()
                .map(columnMapper::column)
                .collect(Collectors.joining(COLUMN_SEPARATOR));
    }

    public String drop(Class<?> clazz) {
        String tableName = getTableName(clazz);
        return String.format(DROP_QUERY_FORMAT, tableName);
    }
}
