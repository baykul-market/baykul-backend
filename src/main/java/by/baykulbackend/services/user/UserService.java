package by.baykulbackend.services.user;

import by.baykulbackend.config.PasswordEncoderConfig;
import by.baykulbackend.database.dao.balance.Balance;
import by.baykulbackend.database.dao.cart.Cart;
import by.baykulbackend.database.dao.user.Profile;
import by.baykulbackend.database.dao.user.User;
import by.baykulbackend.database.model.Role;
import by.baykulbackend.database.repository.user.IRefreshTokenRepository;
import by.baykulbackend.database.repository.user.IUserRepository;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.utils.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final IUserRepository iUserRepository;
    private final IRefreshTokenRepository iRefreshTokenRepository;
    private final AuthService authService;
    private final PasswordEncoderConfig passwordEncoderConfig;

    // TODO: realize user's profile
    /**
     * Creates a new user in the system.
     * Validates input.
     *
     * @param user the User object to create
     * @return ResponseEntity with success/error message
     */
    public ResponseEntity<?> createUser(User user) {
        Map<String, String> response = new HashMap<>();

        if (isNotValidNewUser(user, response)) {
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (hasNotUniqueData(user, response)) {
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }

        user.setPassword(passwordEncoderConfig.getPasswordEncoder().encode(user.getPassword()));

        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }

        if (user.getBlocked() == null) {
            user.setBlocked(false);
        }

        Profile profile = new Profile();
        profile.setUser(user);
        user.setProfile(profile);

        Balance balance = new Balance();
        balance.setUser(user);
        balance.setAccount(new BigDecimal("0.00"));
        user.setBalance(balance);

        Cart cart = new Cart();
        cart.setUser(user);
        user.setCart(cart);

        iUserRepository.save(user);
        response.put("create_user", "true");
        log.info("User {} has ben created.", user.getLogin());

        return ResponseEntity.ok(response);
    }

    /**
     * Registers a new user in the system with USER role.
     *
     * @param user the User object containing registration details
     * @return ResponseEntity with success message or validation errors
     */
    public ResponseEntity<?> registerUser(User user) {
        user.setRole(Role.USER);
        user.setBlocked(false);

        return createUser(user);
    }

    /**
     * Deletes a user by their ID.
     *
     * @param id the UUID of the user to delete
     * @return ResponseEntity with success/error message
     * @throws NotFoundException if no user is found with the given ID
     */
    public ResponseEntity<?> deleteUserById(UUID id) {
        Map<String, String> response = new HashMap<>();
        User userFromDB = iUserRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));

        iUserRepository.deleteById(id);
        response.put("delete_user", "true");
        log.info("Delete user with id = {} -> {}", id, authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing user's information taking user to update from authentication principal.
     * Only updates non-null fields from the provided user object.
     *
     * @param user the User object containing updated fields
     * @return ResponseEntity with success/error message
     * @throws NotFoundException if no user is found with authentication principal
     */
    public ResponseEntity<?> updateProfile(User user) {
        User userFromDB = iUserRepository.findByLogin(authService.getAuthInfo().getPrincipal().toString())
                .orElseThrow(() -> new NotFoundException("User not found"));

        return updateUser(userFromDB, user);
    }

    /**
     * Updates an existing user's information.
     * Only updates non-null fields from the provided user object.
     *
     * @param id   the UUID of the user to update
     * @param user the User object containing updated fields
     * @return ResponseEntity with success/error message
     * @throws NotFoundException if no user is found with the given ID
     */
    public ResponseEntity<?> updateUserById(UUID id, User user) {
        User userFromDB = iUserRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));

        return updateUser(userFromDB, user);
    }

    /**
     * Updates an existing user's information.
     * Only updates non-null fields from the provided user object.
     * Validates input.
     *
     * @param userFromDB the User object to update
     * @param user       the User object containing updated fields
     * @return ResponseEntity with success/error message
     */
    private ResponseEntity<?> updateUser(User userFromDB, User user) {
        Map<String, String> response = new HashMap<>();
        UUID id = userFromDB.getId();

        if (isNotValidUser(user, response)) {
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (hasNotUniqueData(user, response)) {
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }

        if (user.getLogin() != null) {
            userFromDB.setLogin(user.getLogin());
            log.info("User's login with id {} has been updated -> {}",
                    id, authService.getAuthInfo().getPrincipal());
        }

        if (user.getEmail() != null) {
            userFromDB.setEmail(user.getEmail());
            log.info("User's email with id {} has been updated -> {}",
                    id, authService.getAuthInfo().getPrincipal());
        }

        if (user.getPhoneNumber() != null) {
            userFromDB.setPhoneNumber(user.getPhoneNumber());
            log.info("User's phone number with id {} has been updated -> {}",
                    id, authService.getAuthInfo().getPrincipal());
        }

        if (user.getPassword() != null) {
            // Delete all refresh tokens for security
            userFromDB.setPassword(passwordEncoderConfig.getPasswordEncoder().encode(user.getPassword()));
            iRefreshTokenRepository.deleteByUser(userFromDB);
            log.info("The password of User with the id {} has been updated -> {}",
                    id, authService.getAuthInfo().getPrincipal());
        }

        if (user.getBlocked() != userFromDB.getBlocked()) {
            userFromDB.setBlocked(user.getBlocked());

            if (user.getBlocked()) {
                iRefreshTokenRepository.deleteByUser(userFromDB);
                log.info("The sessions of User with the id {} has been deleted -> {}",
                        id, authService.getAuthInfo().getPrincipal());
            }

            log.info("The blocked of User with the id {} has been updated -> {}",
                    id, authService.getAuthInfo().getPrincipal());
        }

        iUserRepository.save(userFromDB);
        response.put("update_user", "true");

        return ResponseEntity.ok(response);
    }

    /**
     * Searches for users by login, email, or phone number containing the given text with pagination.
     * Performs case-insensitive search for login and email, case-insensitive search for phone number.
     * Returns distinct users to avoid duplicates.
     *
     * @param text the search text to match against user attributes
     * @param pageable pagination and sorting parameters
     * @return Page of matching User objects
     */
    public Page<User> searchUser(String text, Pageable pageable) {
        if (text == null || text.isEmpty()) {
            return Page.empty(pageable);
        }

        Set<User> resultSet = new LinkedHashSet<>();

        // Поиск по логину
        resultSet.addAll(iUserRepository.findByLoginContainingIgnoreCase(text, Pageable.unpaged()).getContent());

        // Поиск по email
        resultSet.addAll(iUserRepository.findByEmailContainingIgnoreCase(text, Pageable.unpaged()).getContent());

        // Поиск по телефону
        resultSet.addAll(iUserRepository.findByPhoneNumberContaining(text, Pageable.unpaged()).getContent());

        List<User> content = new ArrayList<>(resultSet);

        // Ручная пагинация
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), content.size());

        if (start > content.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, content.size());
        }

        List<User> pageContent = content.subList(start, end);
        return new PageImpl<>(pageContent, pageable, content.size());
    }

    /**
     * Validates a uniqueness of new user data.
     *
     * @param user the User object to validate
     * @param response Map to collect validation error messages
     * @return true if user data is unique, false otherwise
     */
    private boolean hasNotUniqueData(User user, Map<String, String> response) {
        if (user.getLogin() != null) {
            Optional<User> userFoundByLoginOptional = iUserRepository.findByLogin(user.getLogin());

            if (userFoundByLoginOptional.isPresent() && !userFoundByLoginOptional.get().getId().equals(user.getId())) {
                response.put("error_login", "User with that login already exists");
                log.warn("User with login '{}' already exists", user.getLogin());
                return true;
            }
        }

        if (user.getEmail() != null) {
            Optional<User> userFoundByEmailOptional = iUserRepository.findByEmail(user.getEmail());

            if (userFoundByEmailOptional.isPresent() && !userFoundByEmailOptional.get().getId().equals(user.getId())) {
                response.put("error_email", "User with that email already exists");
                log.warn("User with email '{}' already exists", user.getEmail());
                return true;
            }
        }

        if (user.getPhoneNumber() != null) {
            Optional<User> userFoundByPhoneNumberOptional = iUserRepository.findByPhoneNumber(user.getPhoneNumber());

            if (userFoundByPhoneNumberOptional.isPresent() && !userFoundByPhoneNumberOptional.get().getId()
                    .equals(user.getId())) {
                response.put("error_phone_number", "User with that phone number already exists");
                log.warn("User with phone number '{}' already exists", user.getPhoneNumber());
                return true;
            }
        }

        return false;
    }

    /**
     * Validates a new user request.
     *
     * @param user the User object to validate
     * @param response Map to collect validation error messages
     * @return true if user is valid for creation, false otherwise
     */
    private boolean isNotValidNewUser(User user, Map<String, String> response) {
        if (StringUtils.isBlank(user.getLogin())) {
            response.put("error_login", "The login must not be empty");
            log.warn("The login must not be empty");
            return true;
        }

        if (StringUtils.isBlank(user.getPassword())) {
            response.put("error_password", "The password must not be empty");
            log.warn("The password must not be empty");
            return true;
        }

        if (StringUtils.isBlank(user.getEmail()) && StringUtils.isBlank(user.getPhoneNumber())) {
            response.put("error_data", "One of the following must be filled in: email, phone number");
            log.warn("One of the following must be filled in: email, phone number");
            return true;
        }

        return isNotValidUser(user, response);
    }

    /**
     * Validates a user request.
     *
     * @param user the User object to validate
     * @param response Map to collect validation error messages
     * @return true if user data is valid, false otherwise
     */
    private boolean isNotValidUser(User user, Map<String, String> response) {
        if (StringUtils.isNotBlank(user.getLogin())) {
            if (user.getLogin().length() < 3) {
                response.put("error_login", "The login must be at least 3 characters");
                log.warn("The login must be at least 3 characters");
                return true;
            }

            if (user.getLogin().length() > 50) {
                response.put("error_login", "The login must be at most 50 characters");
                log.warn("The login must be at most 50 characters");
                return true;
            }
        }

        if (StringUtils.isNotBlank(user.getPassword()) && user.getPassword().length() < 8) {
            response.put("error_password", "The password must be at least 8 characters");
            log.warn("The password must be at least 8 characters");
            return true;
        }


        if (StringUtils.isNotBlank(user.getPhoneNumber()) && !Validator.isValidPhoneNumber(user.getPhoneNumber())) {
            response.put("error_phone_number", "Invalid phone number");
            log.warn("Invalid phone number");
            return true;
        }

        return false;
    }
}
