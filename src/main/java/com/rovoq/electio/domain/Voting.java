package com.rovoq.electio.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "voting")
@Getter
@Setter
public class Voting {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp start;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp stop;
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "creator_id")
    private User creator;
    private String creationDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "voting_id")
    private Set<Answer> answer = new HashSet<>();
}
