package com.koreanmarkers.assignment.login_app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 유저 상세 정보 - 실명, 핸드폰번호, 성별, 이메일 ... ㄴ
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDetail extends BaseTimeEntity{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;

    @Column
    private String email;

    @Setter
    @OneToOne(fetch = FetchType.LAZY)
    private User user;

    @Builder
    public UserDetail(String name, String email, User user) {
        this.name = name;
        this.email = email;
        this.user = user;
    }

}
