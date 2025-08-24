package com.jaiswarsecurities.core.service;

import java.util.List;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jaiswarsecurities.core.dto.auth.PasswordChangeRequest;
import com.jaiswarsecurities.core.dto.auth.UserProfileDto;
import com.jaiswarsecurities.core.exception.AuthenticationException;
import com.jaiswarsecurities.core.exception.UserNotFoundException;
import com.jaiswarsecurities.core.model.User;
import com.jaiswarsecurities.core.repository.UserRepository;
import com.jaiswarsecurities.core.dao.UserDao;
import com.jaiswarsecurities.core.service.TradeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * User service implementing UserDetailsService for Spring Security integration
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService implements UserDetailsService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final ModelMapper modelMapper;
    @Autowired
    private UserDao userDao;
    @Autowired
    private TradeService tradeService;

	/**
	 * Load user by username for Spring Security
	 */
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		log.debug("Loading user by username: {}", username);

		//return userRepository.findByUsernameOrEmail(username)
		//		.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        User user = userDao.getUserByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return user;
	}

	/**
	 * Get user profile by ID
	 */
	public UserProfileDto getUserProfile(UUID userId) {
		log.debug("Getting user profile for ID: {}", userId);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

		return modelMapper.map(user, UserProfileDto.class);
	}

	/**
	 * Update user profile
	 */
	public UserProfileDto updateUserProfile(UUID userId, UserProfileDto profileDto) {
		log.info("Updating user profile for ID: {}", userId);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

		// Update allowed fields
		if (profileDto.getFirstName() != null) {
			user.setFirstName(profileDto.getFirstName());
		}
		if (profileDto.getLastName() != null) {
			user.setLastName(profileDto.getLastName());
		}
		if (profileDto.getPhoneNumber() != null) {
			user.setPhoneNumber(profileDto.getPhoneNumber());
		}

		User updatedUser = userRepository.save(user);
		log.info("User profile updated successfully for: {}", user.getUsername());

		return modelMapper.map(updatedUser, UserProfileDto.class);
	}

	/**
	 * Change user password
	 */
	public void changePassword(UUID userId, PasswordChangeRequest request) {
		log.info("Changing password for user ID: {}", userId);

		if (!request.isNewPasswordMatching()) {
			throw new AuthenticationException("New passwords do not match");
		}

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

		// Verify current password
		if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
			throw new AuthenticationException("Current password is incorrect");
		}

		// Update password
		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		userRepository.save(user);

		log.info("Password changed successfully for user: {}", user.getUsername());
	}

	/**
	 * Get all users with pagination
	 */
	public Page<UserProfileDto> getAllUsers(Pageable pageable) {
		log.debug("Getting all users with pagination");

		Page<User> users = userRepository.findAll(pageable);
		return users.map(user -> modelMapper.map(user, UserProfileDto.class));
	}

	/**
	 * Search users
	 */
	public Page<UserProfileDto> searchUsers(String searchTerm, Pageable pageable) {
		log.debug("Searching users with term: {}", searchTerm);

		Page<User> users = userRepository.searchUsers(searchTerm, pageable);
		return users.map(user -> modelMapper.map(user, UserProfileDto.class));
	}

	/**
	 * Delete user account
	 */
	public void deleteUser(UUID userId) {
		log.info("Deleting user with ID: {}", userId);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

		userRepository.delete(user);
		log.info("User deleted successfully: {}", user.getUsername());
	}

	/**
	 * Check if username is available
	 */
	public boolean isUsernameAvailable(String username) {
		return !userRepository.existsByUsername(username);
	}

	/**
	 * Check if email is available
	 */
	public boolean isEmailAvailable(String email) {
		return !userRepository.existsByEmail(email);
	}

	/**
	 * Get user by username
	 */
	public UserProfileDto getUserByUsername(String username) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new UserNotFoundException("User not found: " + username));

		return modelMapper.map(user, UserProfileDto.class);
	}

	/**
	 * Get user by email
	 */
	public UserProfileDto getUserByEmail(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

		return modelMapper.map(user, UserProfileDto.class);
	}

	/**
	 * Unlock user account (admin function)
	 */
	public void unlockAccount(UUID userId) {
		log.info("Unlocking account for user ID: {}", userId);

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

		user.unlockAccount();
		userRepository.save(user);

		log.info("Account unlocked successfully for user: {}", user.getUsername());
	}

	/**
	 * Get locked accounts
	 */
	public List<User> getLockedAccounts() {
		return userRepository.findLockedAccounts();
	}

	/**
	 * Count users registered today
	 */
	public long countUsersRegisteredToday() {
		return userRepository.countUsersRegisteredToday();
	}

    public void insertTradeData(LocalDateTime timestamp, String symbol, double price, long quantity) {
        tradeService.insertTradeAsync(timestamp, symbol, price, quantity);
    }

    public void insertUser(User user) {
        userDao.insertUser(user);
    }

    public User getUserByEmail(String email) {
        return userDao.getUserByEmail(email);
    }

    public void migrateUsersToClickHouse() {
        List<User> users = userRepository.findAll();
        users.forEach(user -> userDao.insertUser(user));
    }

    public void batchInsertTrades(List<Object[]> trades) {
        tradeService.batchInsertTrades(trades);
    }
}