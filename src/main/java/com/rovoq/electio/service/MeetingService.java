package com.rovoq.electio.service;

import com.rovoq.electio.domain.*;
import com.rovoq.electio.repos.AnswerRepo;
import com.rovoq.electio.repos.MeetingRepo;
import com.rovoq.electio.repos.VoteRepo;
import com.rovoq.electio.repos.VotingRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.validation.constraints.Null;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MeetingService {
    @Autowired
    private MeetingRepo meetingRepo;

    @Autowired
    private VotingRepo votingRepo;

    @Autowired
    private AnswerRepo answerRepo;

    @Autowired
    private VoteRepo voteRepo;

    public void createMeeting(User user, String name, String description, Boolean lock){
        Meeting meeting = new Meeting();
        meeting.setName(name);
        meeting.setDescription(description);
        meeting.setCreator(user.getUsername());
        meeting.setLocked(lock);
        meeting.setCreationDate(new SimpleDateFormat("yyyy.MM.dd").format(new Date()));
        subscribe(user, meeting);
    }

    public void subscribe(User user, Meeting meeting){
        Set<User> subscribers = meeting.getSubscribers();

        if (!subscribers.contains(user)){
            subscribers.add(user);
        }

        meetingRepo.save(meeting);
    }

    public void edit(User user, Meeting meeting, String name, String description, Boolean locked){
        if(user.getUsername().equals(meeting.getCreator())){
            meeting.setName(name);
            meeting.setDescription(description);
            meeting.setLocked(locked);

            meetingRepo.save(meeting);
        }
    }

    public void createVoting(String name, String description, Meeting meeting){
        Voting voting = new Voting();
        voting.setName(name);
        voting.setDescription(description);
        votingRepo.save(voting);

        Set<Voting> voting_subscribers = meeting.getVotingSubscribers();
        voting_subscribers.add(voting);
        meetingRepo.save(meeting);
    }

    public void createAnswer(String name, Meeting meeting, Voting voting){
        Answer answer = new Answer();
        answer.setName(name);
        answerRepo.save(answer);

        Set<Answer> answer_subscribers = voting.getAnswerSubscribers();
        answer_subscribers.add(answer);
        votingRepo.save(voting);
    }

    public void vote(User user, Meeting meeting, Voting voting, Answer answer){

        Set<Vote> vote_subscribers = answer.getVoteSubscribers();
        Set<Answer> answer_subscribers = voting.getAnswerSubscribers();
        Set<String> usernames = new HashSet<>();

        for(Answer answer1 : answer_subscribers){
            Set<Vote> vote_subscribers1 = answer1.getVoteSubscribers();
            for(Vote vote1 : vote_subscribers1){
                if (!usernames.contains(vote1.getUsername())){
                    usernames.add(vote1.getUsername());
                }
            }
        }
        if (!usernames.contains(user.getUsername())){
            Vote vote = new Vote();
            vote.setUsername(user.getUsername());
            voteRepo.save(vote);

            vote_subscribers.add(vote);
            answerRepo.save(answer);
        }

    }

    public List<Meeting> findAllMeeting(){
        return meetingRepo.findAll();
    }
    public List<Voting> findAllVoting(){
        return votingRepo.findAll();
    }

}
