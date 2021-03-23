package com.rovoq.electio.controller;

import com.rovoq.electio.domain.*;
import com.rovoq.electio.service.MeetingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.crypto.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.*;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/meeting")
public class MeetingController {

    @Autowired
    MeetingService meetingService;

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
        if (lock.equals("open")){
            locked = false;
        } else {
            locked = true;
        }

        meetingService.createMeeting(user, name, description, locked);

        return "redirect:/meeting/list";
    }

    @GetMapping("{meetingId}")
    public String getMeeting(@AuthenticationPrincipal User user, @PathVariable("meetingId") Meeting meeting, Model model) {
        model.addAttribute("meeting",meeting);
        model.addAttribute("user", user);
        model.addAttribute("votings", meeting.getVotingSubscribers());
//        model.addAttribute("votings", meetingService.findAllVoting());

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
        if (lock.equals("open")){
            locked = false;
        } else {
            locked = true;
        }

        meetingService.edit(user, meeting, name, description, locked);

        return "redirect:/meeting/" + meeting.getId();
    }

    @GetMapping("{meetingId}/createVoting")
    public String getVotingCreation() {
        return "createVoting";
    }

    @PostMapping("{meetingId}/createVoting")
    public String createVoting(
            @RequestParam String name,
            @RequestParam String description,
            @PathVariable("meetingId") Meeting meeting) {

        meetingService.createVoting(name, description, meeting);

        return "redirect:/meeting/" + meeting.getId();
    }

    @GetMapping("{meetingId}/{votingId}/createAnswer")
    public String getAnswerCreator() {
        return "createAnswer";
    }

    @PostMapping("{meetingId}/{votingId}/createAnswer")
    public String createAnswer(
            @PathVariable("meetingId") Meeting meeting,
            @PathVariable("votingId") Voting voting,
            @RequestParam String name) {

        meetingService.createAnswer(name, meeting, voting);

        return "redirect:/meeting/" + meeting.getId() + "/" + voting.getId();
    }

    @GetMapping("{meetingId}/{votingId}")
    public String getVoting (
            @AuthenticationPrincipal User user,
            @PathVariable("meetingId") Meeting meeting,
            @PathVariable("votingId") Voting voting,
            Model model){

        model.addAttribute("user", user);
        model.addAttribute("meeting", meeting);
        model.addAttribute("voting", voting);
        model.addAttribute("answers", voting.getAnswerSubscribers());

        return "voting";
    }

    @GetMapping("{meetingId}/{votingId}/{answerId}/vote")
    public String vote(
            @AuthenticationPrincipal User user,
            @PathVariable("meetingId") Meeting meeting,
            @PathVariable("votingId") Voting voting,
            @PathVariable("answerId") Answer answer) {

        meetingService.vote(user, meeting, voting, answer);

        return "redirect:/meeting/" + meeting.getId() + "/" + voting.getId();
    }











    @GetMapping("{meetingId}/{votingId}/test")
    public String getVotingTest (
            @PathVariable("meetingId") Meeting meeting,
            @PathVariable("votingId") Voting voting) throws Exception {

        // Generate keys
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        SecureRandom random = SecureRandom.getInstanceStrong();
        generator.initialize(2048, random);
        KeyPair keyPair = generator.generateKeyPair();

        // Digital Signature
        Signature dsa = Signature.getInstance("SHA256withRSA");
        dsa.initSign(keyPair.getPrivate());

        // Update and sign the data
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
        byte[] data = cipher.doFinal(voting.toString().getBytes());
        dsa.update(data);
        byte[] signature = dsa.sign();

        // Verify signature
        dsa.initVerify(keyPair.getPublic());
        dsa.update(data);
        boolean verifies = dsa.verify(signature);
        System.out.println("Signature is ok: " + verifies);

        // Decrypt if signature is correct
        if (verifies) {
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] result = cipher.doFinal(data);
            System.out.println(new String(result));
            System.out.println(new String(result).equals(voting.toString()));
        }

        return "voting";
    }
}
