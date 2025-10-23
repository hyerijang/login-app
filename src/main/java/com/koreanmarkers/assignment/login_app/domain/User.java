	package com.koreanmarkers.assignment.login_app.domain;

	import jakarta.persistence.CascadeType;
	import jakarta.persistence.Column;
	import jakarta.persistence.Entity;
	import jakarta.persistence.EnumType;
	import jakarta.persistence.Enumerated;
	import jakarta.persistence.GeneratedValue;
	import jakarta.persistence.GenerationType;
	import jakarta.persistence.Id;
	import jakarta.persistence.JoinColumn;
	import jakarta.persistence.OneToMany;
	import jakarta.persistence.OneToOne;
	import jakarta.persistence.PreUpdate;
	import jakarta.persistence.Table;
	import java.time.LocalDateTime;
	import java.util.ArrayList;
	import java.util.List;
	import lombok.AccessLevel;
	import lombok.Builder;
	import lombok.Getter;
	import lombok.NoArgsConstructor;
	import lombok.Setter;

	/**
	 * 유저 기본 정보
	 */
	@Setter
	@Getter
	@Entity
	@Table(name = "users")
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public class User extends BaseTimeEntity{

	    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

		@Column
		private String nickname;

	    @Column
	    private String pictureUrl;

	    @Column
	    @Enumerated(EnumType.STRING)
	    private UserRole role;

		@OneToOne(cascade = CascadeType.ALL)
		@JoinColumn(name = "user_detail_id")
		private UserDetail userDetail;

	    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
		private List<UserSocial> authProviders = new ArrayList<>();

		@OneToOne(cascade = CascadeType.ALL)
		@JoinColumn(name = "user_account_id")
		private UserAccount userAccount;

		// === Builder===

		@Builder
		public User(String nickname, String pictureUrl, UserRole role) {
			this.nickname = nickname;
			this.pictureUrl = pictureUrl;
			this.role = role;
		}

		// === 연관관계 편의 메서드 ===
		public void setUserDetail(UserDetail userDetail) {
			this.userDetail = userDetail;
			userDetail.setUser(this);
		}

		public void addAuthProvider(UserSocial userSocial) {
			this.authProviders.add(userSocial);
			userSocial.setUser(this);
		}

		public void setUserAccount(UserAccount userAccount) {
			this.userAccount = userAccount;
			userAccount.setUser(this);
		}
	}