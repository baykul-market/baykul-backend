package by.baykulbackend.services.user;

import by.baykulbackend.config.PasswordEncoderConfig;
import by.baykulbackend.database.dao.balance.Balance;
import by.baykulbackend.database.dao.cart.Cart;
import by.baykulbackend.database.dao.user.Localization;
import by.baykulbackend.database.dao.user.Profile;
import by.baykulbackend.database.dao.user.User;
import by.baykulbackend.database.model.Role;
import by.baykulbackend.database.repository.order.IOrderRepository;
import by.baykulbackend.database.repository.user.IRefreshTokenRepository;
import by.baykulbackend.database.repository.user.IUserRepository;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.database.dto.user.UserPatchRequest;
import by.baykulbackend.services.finance.PriceService;
import by.baykulbackend.utils.Validator;
import by.baykulbackend.services.email.EmailService;
import org.thymeleaf.context.Context;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final IUserRepository iUserRepository;
    private final IRefreshTokenRepository iRefreshTokenRepository;
    private final IOrderRepository iOrderRepository;
    private final AuthService authService;
    private final PasswordEncoderConfig passwordEncoderConfig;
    private final PriceService priceService;
    private final EmailService emailService;

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

        if (user.getCanPayLater() == null) {
            user.setCanPayLater(false);
        }

        if (user.getLocalization() == null) {
            user.setLocalization(Localization.RUS);
        }

        Profile profile = user.getProfile();

        if (profile == null) {
            profile = new Profile();
            user.setProfile(profile);
        }

        profile.setUser(user);

        Balance balance = new Balance();
        balance.setUser(user);
        balance.setAccount(new BigDecimal("0.00"));
        balance.setCurrency(priceService.getSystemCurrency());
        user.setBalance(balance);

        Cart cart = new Cart();
        cart.setUser(user);
        user.setCart(cart);

        iUserRepository.save(user);
        response.put("create_user", "true");
        log.info("User {} has been created.", user.getLogin());

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
        user.setBlocked(true);
        user.setCanPayLater(null);
        user.setMarkupPercentage(null);

        ResponseEntity<?> response = createUser(user);

        if (response.getStatusCode().is2xxSuccessful()) {
            List<User> admins = iUserRepository.findAllByRole(Role.ADMIN);
            for (User admin : admins) {
                if (admin.getEmail() != null) {
                    Context context = new Context();
                    context.setVariable("login", user.getLogin() != null ? user.getLogin() : "N/A");
                    context.setVariable("email", user.getEmail() != null ? user.getEmail() : "N/A");
                    context.setVariable("phone", user.getPhoneNumber() != null ? user.getPhoneNumber() : "N/A");
                    emailService.sendEmail(admin.getEmail(), "New User Registered", "new-user-registration", admin.getLocalization() != null ? admin.getLocalization() : Localization.RUS, context);
                }
            }
        }

        return response;
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
        User user = iUserRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));

        iOrderRepository.deleteAllByUserId(user.getId());
        iUserRepository.deleteById(id);
        response.put("delete_user", "true");
        log.info("Delete user with id = {} -> {}", id, authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing user's information taking user to update from authentication principal.
     * Only updates non-null fields from the provided patch object.
     * Sensitive fields like role, blocked, and markupPercentage are cleared to prevent unauthorized changes.
     *
     * @param patch the UserPatchRequest object containing updated fields
     * @return ResponseEntity with success/error message
     * @throws NotFoundException if no user is found with authentication principal
     */
    public ResponseEntity<?> updateProfile(UserPatchRequest patch) {
        User userFromDB = iUserRepository.findByLogin(authService.getAuthInfo().getPrincipal().toString())
                .orElseThrow(() -> new NotFoundException("User not found"));

        patch.setBlocked(null);
        patch.setCanPayLater(null);
        patch.setMarkupPercentage(null);
        patch.setRole(null);
        patch.setPhoneNumber(null);
        patch.setEmail(null);

        return updateUserById(userFromDB.getId(), patch);
    }

    /**
     * Updates an existing user's information from a dedicated patch DTO.
     *
     * <p>This overload is the <em>robust</em> path for admin PATCH requests because
     * {@link UserPatchRequest#getMarkupPercentage()} carries three distinct states:
     * <ul>
     *   <li>{@code null} – field was absent in JSON → do not touch the stored value.</li>
     *   <li>{@code Optional.empty()} – field was JSON {@code null} → clear to {@code null}
     *       so the global pricing fallback is used.</li>
     *   <li>{@code Optional.of(v)} – set to {@code v}.</li>
     * </ul>
     *
     * @param id    the UUID of the user to update
     * @param patch the patch DTO containing fields to update
     * @return ResponseEntity with success/error message
     * @throws NotFoundException if no user is found with the given ID
     */
    public ResponseEntity<?> updateUserById(UUID id, UserPatchRequest patch) {
        User userFromDB = iUserRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        Map<String, String> response = new HashMap<>();
        boolean updated = false;

        if (checkUniqueData(patch.getLogin(), patch.getEmail(), patch.getPhoneNumber(), id, response)) {
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }

        if (patch.getLogin() != null) {
            updated |= updateField(patch.getLogin(), userFromDB.getLogin(), userFromDB::setLogin, "login", id);
        }

        if (patch.getEmail() != null) {
            String newEmail = StringUtils.isBlank(patch.getEmail()) ? null : patch.getEmail();
            if (newEmail == null) {
                boolean willHavePhone = patch.getPhoneNumber() != null 
                        ? !StringUtils.isBlank(patch.getPhoneNumber()) 
                        : !StringUtils.isBlank(userFromDB.getPhoneNumber());
                if (!willHavePhone) {
                    response.put("error_email", "One of the following must be filled in: email, phone number");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
            }
            updated |= updateField(newEmail, userFromDB.getEmail(), userFromDB::setEmail, "email", id);
        }

        if (patch.getPhoneNumber() != null) {
            String newPhone = StringUtils.isBlank(patch.getPhoneNumber()) ? null : patch.getPhoneNumber();
            if (newPhone == null) {
                boolean willHaveEmail = patch.getEmail() != null 
                        ? !StringUtils.isBlank(patch.getEmail()) 
                        : !StringUtils.isBlank(userFromDB.getEmail());
                if (!willHaveEmail) {
                    response.put("error_phone_number", "One of the following must be filled in: email, phone number");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
            }
            updated |= updateField(newPhone, userFromDB.getPhoneNumber(), userFromDB::setPhoneNumber, "phone number", id);
        }

        if (patch.getPassword() != null) {
            userFromDB.setPassword(passwordEncoderConfig.getPasswordEncoder().encode(patch.getPassword()));
            iRefreshTokenRepository.deleteByUser(userFromDB);
            updated = true;
            log.info("The password of User with the id {} has been updated -> {}", id, authService.getAuthInfo().getPrincipal());
        }

        if (patch.getRole() != null) {
            updated |= updateField(patch.getRole(), userFromDB.getRole(), userFromDB::setRole, "role", id);
        }

        if (patch.getBlocked() != null && !patch.getBlocked().equals(userFromDB.getBlocked())) {
            userFromDB.setBlocked(patch.getBlocked());
            if (patch.getBlocked()) {
                iRefreshTokenRepository.deleteByUser(userFromDB);
                log.info("The sessions of User with the id {} has been deleted -> {}", id, authService.getAuthInfo().getPrincipal());
            }
            updated = true;
            log.info("The blocked of User with the id {} has been updated -> {}", id, authService.getAuthInfo().getPrincipal());
        }

        if (patch.getCanPayLater() != null) {
            updated |= updateField(patch.getCanPayLater(), userFromDB.getCanPayLater(), userFromDB::setCanPayLater, "canPayLater", id);
        }

        if (patch.getLocalization() != null) {
            updated |= updateField(patch.getLocalization(), userFromDB.getLocalization(), userFromDB::setLocalization, "localization", id);
        }

        if (patch.getMarkupPercentage() != null) {
            BigDecimal newMarkup = patch.getMarkupPercentage().orElse(null);
            updated |= updateField(newMarkup, userFromDB.getMarkupPercentage(), userFromDB::setMarkupPercentage, "markup percentage", id);
        }

        if (patch.getProfile() != null) {
            Profile profile = patch.getProfile();
            Profile profileFromDb = userFromDB.getProfile();
            if (profileFromDb == null) {
                profileFromDb = new Profile();
                profileFromDb.setUser(userFromDB);
                userFromDB.setProfile(profileFromDb);
            }

            if (profile.getSurname() != null) {
                String clean = StringUtils.isBlank(profile.getSurname()) ? null : profile.getSurname();
                updated |= updateField(clean, profileFromDb.getSurname(), profileFromDb::setSurname, "surname", id);
            }
            if (profile.getName() != null) {
                String clean = StringUtils.isBlank(profile.getName()) ? null : profile.getName();
                updated |= updateField(clean, profileFromDb.getName(), profileFromDb::setName, "name", id);
            }
            if (profile.getPatronymic() != null) {
                String clean = StringUtils.isBlank(profile.getPatronymic()) ? null : profile.getPatronymic();
                updated |= updateField(clean, profileFromDb.getPatronymic(), profileFromDb::setPatronymic, "patronymic", id);
            }
        }

        if (updated) {
            iUserRepository.save(userFromDB);
        }

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
     * Checks if the provided login, email, and phone number are already in use by another user.
     * 
     * @param login the login to check for uniqueness
     * @param email the email to check for uniqueness
     * @param phone the phone number to check for uniqueness
     * @param id the UUID of the user being updated (null if creating a new user) to ignore self-matches
     * @param response Map to collect validation error messages
     * @return true if any of the provided data is not unique, false otherwise
     */
    private boolean checkUniqueData(String login, String email, String phone, UUID id, Map<String, String> response) {
        if (login != null) {
            Optional<User> existing = iUserRepository.findByLogin(login);
            if (existing.isPresent() && (id == null || !existing.get().getId().equals(id))) {
                response.put("error_login", "User with that login already exists");
                log.warn("User with login '{}' already exists", login);
                return true;
            }
        }

        if (email != null) {
            Optional<User> existing = iUserRepository.findByEmail(email);
            if (existing.isPresent() && (id == null || !existing.get().getId().equals(id))) {
                response.put("error_email", "User with that email already exists");
                log.warn("User with email '{}' already exists", email);
                return true;
            }
        }

        if (phone != null) {
            Optional<User> existing = iUserRepository.findByPhoneNumber(phone);
            if (existing.isPresent() && (id == null || !existing.get().getId().equals(id))) {
                response.put("error_phone_number", "User with that phone number already exists");
                log.warn("User with phone number '{}' already exists", phone);
                return true;
            }
        }

        return false;
    }

    /**
     * Validates a uniqueness of new user data.
     *
     * @param user the User object to validate
     * @param response Map to collect validation error messages
     * @return true if user data is unique, false otherwise
     */
    private boolean hasNotUniqueData(User user, Map<String, String> response) {
        return checkUniqueData(user.getLogin(), user.getEmail(), user.getPhoneNumber(), user.getId(), response);
    }

    /**
     * Helper method to update a single entity field if its value has changed.
     *
     * @param newValue the new value to potentially set
     * @param oldValue the current value of the field
     * @param setter the consumer to execute the update (e.g., a method reference to a setter)
     * @param fieldName the name of the field being updated (used for logging)
     * @param id the UUID of the user whose field is being updated
     * @param <T> the type of the field
     * @return true if the field was updated, false otherwise
     */
    private <T> boolean updateField(T newValue, T oldValue, java.util.function.Consumer<T> setter, String fieldName, UUID id) {
        if (!java.util.Objects.equals(newValue, oldValue)) {
            setter.accept(newValue);
            log.info("The {} of User with the id {} has been updated -> {}", fieldName, id, authService.getAuthInfo().getPrincipal());
            return true;
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

    // TODO: add email validation
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

        if (StringUtils.isNotBlank(user.getPassword())) {
            if (user.getPassword().length() < 8) {
                response.put("error_password", "The password must be at least 8 characters");
                log.warn("The password must be at least 8 characters");
                return true;
            }

            if (user.getPassword().length() > 100) {
                response.put("error_password", "The password must be at most 100 characters");
                log.warn("The password must be at most 100 characters");
                return true;
            }
        }

        if (StringUtils.isNotBlank(user.getPhoneNumber()) && !Validator.isValidPhoneNumber(user.getPhoneNumber())) {
            response.put("error_phone_number", "Invalid phone number");
            log.warn("Invalid phone number");
            return true;
        }

        if (user.getMarkupPercentage() != null && user.getMarkupPercentage().compareTo(BigDecimal.ZERO) < 0) {
            response.put("error_markup_percentage", "Invalid markup percentage");
            log.warn("Invalid markup percentage");
            return true;
        }

        Profile profile = user.getProfile();

        if (profile != null) {
            if (profile.getSurname() != null && profile.getSurname().length() > 50) {
                response.put("error_surname", "Surname must be at most 50 characters");
            }

            if (profile.getName() != null && profile.getName().length() > 50) {
                response.put("error_name", "Name must be at most 50 characters");
            }

            if (profile.getPatronymic() != null && profile.getPatronymic().length() > 50) {
                response.put("error_patronymic", "Patronymic must be at most 50 characters");
            }
        }

        return false;
    }
}
