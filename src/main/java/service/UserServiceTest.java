package service;

import model.User;

public class UserServiceTest {
    public static void main(String[] args) {
        UserService service = new UserService();
        User u1 = new User(0,"a@test.com","123qwerty","USER",null);
        User created = service.create(u1);
        assert created.getId() > 0 : "Create user failed";

        try {
            service.create(new User(0,"a@test.com","pass","USER",null));
            System.err.println("FAIL: Duplicate email");
        } catch (Exception ignored) {
        }

        created.setPassword("321rewq");
        User updated = service.update(created.getId(), created);
        assert updated.getPassword().equals("321rewq") : "Update failed";

        boolean del = service.delete(created.getId());
        assert del : "Delete failed";
    }
}
