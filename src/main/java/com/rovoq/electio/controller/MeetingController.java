package com.rovoq.electio.controller;

import com.rovoq.electio.domain.Answer;
import com.rovoq.electio.domain.Meeting;
import com.rovoq.electio.domain.User;
import com.rovoq.electio.domain.Voting;
import com.rovoq.electio.service.MeetingService;
import com.rovoq.electio.service.UserService;
import org.dom4j.rule.Mode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.Map;

@Controller
@RequestMapping("/meeting")
public class MeetingController {

    @Autowired
    MeetingService meetingService;

    @Autowired
    UserService userService;

    @GetMapping("create")
    public String getMeetingCreator() {
        return "createMeeting";
    }

    @GetMapping("list")
    public String getMeetingList(Model model) {
        model.addAttribute("meetings", meetingService.findAllMeeting());
        return "meetingList";
    }

    @PostMapping("create")
    public String createMeeting(
            @AuthenticationPrincipal User user,
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String lock) {

        boolean locked;
        locked = !lock.equals("open");

        meetingService.createMeeting(user, name, description, locked);

        return "redirect:/meeting/list";
    }

    @GetMapping("{meetingId}")
    public String getMeeting(@AuthenticationPrincipal User user, @PathVariable("meetingId") Meeting meeting, Model model) {
        model.addAttribute("meeting",meeting);
        model.addAttribute("user", user);
        model.addAttribute("votings", meetingService.findByMeeting(meeting));

        return "meeting";
    }

    @GetMapping("{meetingId}/edit")
    public String getMeetingEditor(@PathVariable("meetingId") Meeting meeting, Model model) {
        model.addAttribute("meeting", meeting);
        return "editMeeting";
    }

    @PostMapping("{meetingId}/edit")
    public String editMeeting(
            @AuthenticationPrincipal User user,
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String lock,
            @PathVariable("meetingId") Meeting meeting) {

        boolean locked;
        locked = !lock.equals("open");

        meetingService.edit(user, meeting, name, description, locked);

        return "redirect:/meeting/" + meeting.getId();
    }

    @GetMapping("{meetingId}/createVoting")
    public String getVotingCreation() {
        return "createVoting";
    }

    @PostMapping("{meetingId}/createVoting")
    public String createVoting(
            @AuthenticationPrincipal User user,
            @RequestParam String name,
            @RequestParam String description,
            @PathVariable("meetingId") Meeting meeting,
            @RequestParam Timestamp start,
            @RequestParam Timestamp stop) {
        meetingService.createVoting(user, name, description, meeting, start, stop);

        return "redirect:/meeting/" + meeting.getId();
    }

    @GetMapping("{meetingId}/{votingId}/createAnswer")
    public String getAnswerCreator() {
        return "createAnswer";
    }

    @PostMapping("{meetingId}/{votingId}/createAnswer")
    public String createAnswer(@AuthenticationPrincipal User user,
            @PathVariable("meetingId") Meeting meeting,
            @PathVariable("votingId") Voting voting,
            @RequestParam String name,
            Model model) {

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        if(timestamp.getTime() >= voting.getStart().getTime() & timestamp.getTime() <= voting.getStop().getTime()
                || voting.getCreator().getId().equals(user.getId())){
            meetingService.createAnswer(name, meeting, voting);
        }else{
            model.addAttribute("res","Голосование еще не началось или уже закончилось");
            return "timeOutVoting";
        }

        return "redirect:/meeting/" + meeting.getId() + "/" + voting.getId();
    }

    @GetMapping("{meetingId}/{votingId}")
    public String getVoting (
            @AuthenticationPrincipal User user,
            @PathVariable("meetingId") Meeting meeting,
            @PathVariable("votingId") Voting voting,
            Model model) throws Exception {

        model.addAttribute("user", user);
        model.addAttribute("meeting", meeting);
        model.addAttribute("voting", voting);
        model.addAttribute("answers", meetingService.getAnswers(voting));
        return "voting";
    }

    @GetMapping("{meetingId}/{votingId}/{answerId}/vote")
    public String vote(
            @AuthenticationPrincipal User user,
            @PathVariable("meetingId") Meeting meeting,
            @PathVariable("votingId") Voting voting,
            @PathVariable("answerId") Answer answer,
            Model model) throws Exception {

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        if(timestamp.getTime() >= voting.getStart().getTime() & timestamp.getTime() <= voting.getStop().getTime()
                || voting.getCreator().getId().equals(user.getId())){
            meetingService.vote(user, meeting, voting, answer);
        }else{
            model.addAttribute("res","Голосование еще не началось или уже закончилось");
            return "timeOutVoting";
        }

        return "redirect:/meeting/" + meeting.getId() + "/" + voting.getId();
    }

    @GetMapping("{meetingId}/{votingId}/createResult")
    public String createResult(@AuthenticationPrincipal User user,
                           @PathVariable("meetingId") Meeting meeting,
                           @PathVariable("votingId") Voting voting,
                           Model model) throws Exception {

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        if(timestamp.getTime() > voting.getStop().getTime()
                || voting.getCreator().getId().equals(user.getId())){
            meetingService.createResult(user, meeting, voting);
        }else{
            model.addAttribute("res","Голосование еще не закончилось");
            return "timeOutVoting";
        }

//        return "redirect:/meeting/" + meeting.getId() + "/" + voting.getId();
        return "redirect:/meeting/" + meeting.getId() + "/" + voting.getId() + "/" + "getResult";
    }

    @GetMapping("{meetingId}/{votingId}/getResult")
    public String getResult(@AuthenticationPrincipal User user,
                               @PathVariable("meetingId") Meeting meeting,
                               @PathVariable("votingId") Voting voting,
                               Model model) throws Exception {

        var result = meetingService.getResult(voting);
        var resultVoteMap = meetingService.getVoteMap();

//        for (Map.Entry<String, String> entry : result.entrySet()) {
//            System.out.println(entry.getKey() + " " + entry.getValue());
//        }
//        for (Map.Entry<String, String> entry : resultVoteMap.entrySet()) {
//            System.out.println(entry.getKey() + " " + entry.getValue());
//        }

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        if(timestamp.getTime() > voting.getStop().getTime()){
            model.addAttribute("ProtocolNum", result.get("ProtocolNum"));
            model.addAttribute("Meeting", meeting);
            model.addAttribute("Voting", meetingService.findVotingById(Long.valueOf(result.get("Voting"))).get());
            model.addAttribute("NumVoters", result.get("NumVoters"));
            model.addAttribute("Chairperson", userService.findUserById(Long.valueOf(result.get("Chairperson"))).get());
            model.addAttribute("Votes", resultVoteMap);
            return "result";
        }else{
            model.addAttribute("res","Голосование еще не закончилось");
            return "timeOutVoting";
        }
    }
}
