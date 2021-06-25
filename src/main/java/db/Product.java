package db;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Product {
    private Integer id;
    private String name;
    private String description;
    private String producer;
    private double amount;
    private double price;
    private String nameGroup;

    public Product(Integer id, String name, String description, String producer, double amount, double price, String nameGroup) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.producer = producer;
        this.amount = amount;
        this.price = price;
        this.nameGroup = nameGroup;
    }

    public Product(String name, String description, String producer, double amount, double price, String nameGroup) {
        this.name = name;
        this.description = description;
        this.producer = producer;
        this.amount = amount;
        this.price = price;
        this.nameGroup = nameGroup;
    }

    public Product(ResultSet resultSet) throws SQLException {
        this.id = resultSet.getInt("id");
        this.name = resultSet.getString("name");
        this.description = resultSet.getString("description");
        this.producer = resultSet.getString("producer");
        this.amount = resultSet.getDouble("amount");
        this.price = resultSet.getDouble("price");
        ;
        this.nameGroup = resultSet.getString("g.name");
    }

    public Product() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getNameGroup() {
        return nameGroup;
    }

    public void setNameGroup(String nameGroup) {
        this.nameGroup = nameGroup;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", producer='" + producer + '\'' +
                ", amount=" + amount +
                ", price=" + price +
                ", idGroup=" + nameGroup +
                '}';
    }
}
