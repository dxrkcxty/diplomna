package service;

import model.User;
import repository.UserRepository;
import util.PasswordUtil;

import java.util.List;

public class UserService {
    private final UserRepository userRepository;

    public UserService() {
        this.userRepository = new UserRepository();
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public User getById(long id) {
        return userRepository.findById(id);
    }

    public User create(User user) {
        if (existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (user.getFirstName() == null || user.getFirstName().isBlank()) {
            throw new IllegalArgumentException("Вкажіть ім'я");
        }
        if (user.getLastName() == null || user.getLastName().isBlank()) {
            throw new IllegalArgumentException("Вкажіть прізвище");
        }

        user.setPassword(PasswordUtil.hash(user.getPassword()));

        User created = userRepository.save(user);
        return created;
    }

    public User update(long id, User user) {
        User existing = userRepository.findById(id);
        if (existing == null) {
            return null;
        }

        User existingByEmail = userRepository.findByEmail(user.getEmail());
        if (existingByEmail != null && existingByEmail.getId() != id) {
            throw new IllegalArgumentException("Email already exists");
        }

        user.setId(id);

        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(PasswordUtil.hash(user.getPassword()));
        } else {
            user.setPassword(existing.getPassword());
        }

        if (user.getFirstName() == null || user.getFirstName().isBlank()) {
            user.setFirstName(existing.getFirstName());
        }
        if (user.getLastName() == null || user.getLastName().isBlank()) {
            user.setLastName(existing.getLastName());
        }
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            user.setPhone(existing.getPhone());
        }

        if (user.getBonusRate() == null) {
            user.setBonusRate(existing.getBonusRate());
        }

        User updated = userRepository.save(user);
        return updated;
    }

    public boolean delete(long id) {
        boolean removed = userRepository.deleteById(id);
        return removed;
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
