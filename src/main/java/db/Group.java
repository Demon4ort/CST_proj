package db;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Group {
    private Integer idGroup;
    private String name;
    private String description;

    public Group(Integer idGroup, String name, String description) {
        this.idGroup = idGroup;
        this.name = name;
        this.description = description;
    }

    public Group(String name, String description) {
        this.name = name;
        this.description = description;
    }


    public Group(ResultSet resultSet) throws SQLException {
        this.idGroup = resultSet.getInt("idGroup");
        this.name = resultSet.getString("name");
        this.description = resultSet.getString("description");
    }

    public Group() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getIdGroup() {
        return idGroup;
    }

    public void setIdGroup(Integer idGroup) {
        this.idGroup = idGroup;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Group{" +
                "id=" + idGroup +
                ", name='" + name + '\'' +
                '}';
    }
}
