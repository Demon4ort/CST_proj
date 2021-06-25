package db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqlOps {

    private Connection con;

    public SqlOps(Connection connection) {
        this.con = connection;
    }

    public Connection getCon() {
        return con;
    }

    public Product insertProduct(Product product) {
        Group group = getGroup(product.getNameGroup());
        System.out.println(group);
        try (PreparedStatement statement = con.prepareStatement(
                "INSERT INTO dbKS.Goods(name, description, producer, price, amount, idGroup) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            //statement.setInt(1, 1);
            statement.setString(1, product.getName());
            statement.setString(2, product.getDescription());
            statement.setString(3, product.getProducer());
            statement.setDouble(4, product.getPrice());
            statement.setDouble(5, product.getAmount());
            statement.setInt(6, group.getIdGroup());

            statement.executeUpdate();
            ResultSet resultSet = statement.getGeneratedKeys();
            resultSet.next();
            int id = resultSet.getInt("id");
            product.setId(id);

            statement.close();
            resultSet.close();
            return product;
        } catch (SQLException e) {
            System.out.println("Не вірний SQL запит на вставку");
            e.printStackTrace();
        }
        return product;
    }

    public User insertUser(User user) {
        try (PreparedStatement statement = con.prepareStatement(
                "INSERT INTO dbKS.User(login, password) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, user.getLogin());
            statement.setString(2, user.getPassword());

            statement.executeUpdate();
            ResultSet resultSet = statement.getGeneratedKeys();
            int id = resultSet.getInt("last_insert_rowid()");
            user.setId(id);

            statement.close();
            resultSet.close();
            return user;
        } catch (SQLException e) {
            System.out.println("Не вірний SQL запит на вставку");
            e.printStackTrace();
        }
        return user;
    }

    public Group insertGroup(Group group) {
        try (PreparedStatement statement = con.prepareStatement(
                "INSERT INTO dbKS.groups(dbKS.groups.name, dbKS.groups.description) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, group.getName());
            statement.setString(2, group.getDescription());

            statement.executeUpdate();
            ResultSet resultSet = statement.getGeneratedKeys();
            resultSet.next();
            int id = (int) resultSet.getLong(1);
            group.setIdGroup(id);

            statement.close();
            resultSet.close();
            return group;
        } catch (SQLException e) {
            System.out.println("Не вірний SQL запит на вставку");
            e.printStackTrace();
        }
        return group;
    }

    public Group getGroup(String name) {
        if (name == null) throw new IllegalArgumentException();
        String sql = "SELECT * FROM dbKS.groups WHERE dbKS.groups.name = '" + name + "'";
        try (Statement st = con.createStatement();
             ResultSet res = st.executeQuery(sql)
        ) {
            if (res.next()) {
                return new Group(res);
            }
            return null;
        } catch (SQLException e) {
            System.out.println("Не вірний SQL запит на вибірку даних");
            e.printStackTrace();
            throw new RuntimeException("Can`t select anything", e);
        }
    }

    public Group getGroup(Integer id) {
        if (id == null) throw new IllegalArgumentException();
        String sql = "SELECT * FROM dbKS.groups WHERE dbKS.groups.idGroup = '" + id + "'";
        try (Statement st = con.createStatement();
             ResultSet res = st.executeQuery(sql)
        ) {
            if (res.next()) {
                return new Group(res);
            }
            return null;
        } catch (SQLException e) {
            System.out.println("Не вірний SQL запит на вибірку даних");
            e.printStackTrace();
            throw new RuntimeException("Can`t select anything", e);
        }
    }

    public Product getProduct(Integer id) {
        if (id == null) throw new IllegalArgumentException();
        String sql = "SELECT Goods.id, Goods.name, Goods.price, Goods.producer, amount, g.name, Goods.description " +
                "from dbKS.Goods JOIN dbKS.`groups` g on g.idGroup = Goods.idGroup WHERE Goods.id = " + id;
        try (Statement st = con.createStatement();
             ResultSet res = st.executeQuery(sql)
        ) {
            if (res.next()) {
                return new Product(res);
            }
            return null;
        } catch (SQLException e) {
            System.out.println("Не вірний SQL запит на вибірку даних");
            e.printStackTrace();
            throw new RuntimeException("Can`t select anything", e);
        }
    }

    public User getUser(String login) {
        if (login == null) throw new IllegalArgumentException();
        String sql = "SELECT * FROM dbKS.User WHERE login = '" + login + "'";
        try (Statement st = con.createStatement();
             ResultSet res = st.executeQuery(sql)
        ) {
            if (res.next()) {
                return new User(res);
            }
            return null;
        } catch (SQLException e) {
            System.out.println("Не вірний SQL запит на вибірку даних");
            e.printStackTrace();
            throw new RuntimeException("Can`t select anything", e);
        }

    }

    public void updateProduct(Product product) {
        if (product.getId() == null) throw new IllegalArgumentException();
        Group group = getGroup(product.getNameGroup());
        try (PreparedStatement statement = con.prepareStatement(
                "UPDATE dbKS.Goods SET name=?, price=?, amount=?,description=?,producer=?, idGroup=? WHERE id=?")) {

            statement.setString(1, product.getName());
            statement.setDouble(2, product.getPrice());
            statement.setDouble(3, product.getAmount());
            statement.setString(4, product.getDescription());
            statement.setString(5, product.getProducer());
            statement.setInt(6, group.getIdGroup());
            statement.setInt(7, product.getId());

            statement.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Не вірний SQL запит на update");
            e.printStackTrace();
        }

    }

    public void deleteProduct(Product product) {
        if (product.getId() == null) throw new IllegalArgumentException();
        try (PreparedStatement statement = con.prepareStatement(
                "DELETE FROM dbKS.Goods WHERE id=?")) {

            statement.setInt(1, product.getId());

            statement.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Не вірний SQL запит на delete");
            e.printStackTrace();
        }
    }

    public void deleteProduct(Integer id) {
        if (id == null) throw new IllegalArgumentException();
        try (PreparedStatement statement = con.prepareStatement(
                "DELETE FROM dbKS.Goods WHERE id=?")) {

            statement.setInt(1, id);

            statement.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Не вірний SQL запит на delete");
            e.printStackTrace();
        }
    }

    public void updateGroup(Group group) {
        if (group.getIdGroup() == null) throw new IllegalArgumentException();
        try (PreparedStatement statement = con.prepareStatement(
                "UPDATE dbKS.groups SET dbKS.groups.name=?, dbKS.groups.description=? WHERE idGroup=?")) {

            statement.setString(1, group.getName());
            statement.setString(2, group.getDescription());
            statement.setInt(3, group.getIdGroup());

            statement.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Не вірний SQL запит на update");
            e.printStackTrace();
        }

    }

    public void deleteGroup(Group group) {
        if (group.getIdGroup() == null) throw new IllegalArgumentException();
        try (PreparedStatement statement = con.prepareStatement(
                "DELETE FROM dbKS.groups WHERE idGroup=?")) {

            statement.setInt(1, group.getIdGroup());

            statement.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Не вірний SQL запит на delete");
            e.printStackTrace();
        }
    }

    public List<Product> getAllProduct() {
        try (Statement st = con.createStatement();
             ResultSet res = st.executeQuery("SELECT Goods.id, Goods.name, Goods.price, Goods.producer, amount, g.name, Goods.description " +
                     "from dbKS.Goods JOIN dbKS.`groups` g on g.idGroup = Goods.idGroup")
        ) {
            List<Product> list = new ArrayList<>();
            while (res.next()) {
                list.add(new Product(res));
            }
            return list;
        } catch (SQLException e) {
            System.out.println("Не вірний SQL запит на вибірку даних");
            e.printStackTrace();
            throw new RuntimeException("Can`t select anything", e);
        }
    }

    public List<User> getAllUser() {
        try (Statement st = con.createStatement();
             ResultSet res = st.executeQuery("SELECT * FROM dbKS.User")
        ) {
            List<User> list = new ArrayList<>();
            while (res.next()) {
                list.add(new User(res));
            }
            return list;
        } catch (SQLException e) {
            System.out.println("Не вірний SQL запит на вибірку даних");
            e.printStackTrace();
            throw new RuntimeException("Can`t select anything", e);
        }
    }

    public List<Group> getAllGroup() {
        try (Statement st = con.createStatement();
             ResultSet res = st.executeQuery("SELECT * FROM dbKS.groups")
        ) {
            List<Group> list = new ArrayList<>();
            while (res.next()) {
                list.add(new Group(res));
            }
            return list;
        } catch (SQLException e) {
            System.out.println("Не вірний SQL запит на вибірку даних");
            e.printStackTrace();
            throw new RuntimeException("Can`t select anything", e);
        }
    }


    public void showAllData() {
        try {
            Statement st = con.createStatement();
            ResultSet res = st.executeQuery("SELECT Goods.id, Goods.name, Goods.price, Goods.producer, amount, g.name, Goods.description " +
                    "from dbKS.Goods JOIN dbKS.`groups` g on g.idGroup = Goods.idGroup");
            while (res.next()) {
                String name = res.getString("name");
                System.out.println(res.getShort("id") + " " + name);
            }
            res.close();
            st.close();
        } catch (SQLException e) {
            System.out.println("Не вірний SQL запит на вибірку даних");
            e.printStackTrace();
        }
    }

}
