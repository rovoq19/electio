package com.rovoq.electio.service;

import com.rovoq.electio.domain.*;
import com.rovoq.electio.repos.*;
import com.rovoq.electio.service.signature.SignatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

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

    @Autowired
    private ResultRepo resultRepo;

    @Autowired
    private UserRepo userRepo;

    SignatureService signatureService = new SignatureService("C:\\Users\\aleks\\IdeaProjects\\electio\\ca.p12", "1234", "Test CA");

    Map<String, String> voteMap = new LinkedHashMap<>();

    public MeetingService() throws Exception {
    }

    public void createMeeting(User user, String name, String description, Boolean lock){
        Meeting meeting = new Meeting();
        meeting.setName(name);
        meeting.setDescription(description);
        meeting.setCreator(user);
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
        if(user.equals(meeting.getCreator())){
            meeting.setName(name);
            meeting.setDescription(description);
            meeting.setLocked(locked);

            meetingRepo.save(meeting);
        }
    }

    public void createVoting(User user, String name, String description, Meeting meeting, Timestamp start, Timestamp stop){
        if(user.getId().equals(meeting.getCreator().getId())){
            Voting voting = new Voting();
            voting.setName(name);
            voting.setDescription(description);
            voting.setMeeting(meeting);
            voting.setStart(start);
            voting.setStop(stop);
            voting.setCreator(user);
            voting.setCreationDate(new SimpleDateFormat("yyyy.MM.dd").format(new Date()));
            votingRepo.save(voting);
        }
    }

    public void createAnswer(String name, Meeting meeting, Voting voting){
        Answer answer = new Answer();
        answer.setName(name);
        answer.setVoting(voting);
        answerRepo.save(answer);
    }

    public void vote(User user, Meeting meeting, Voting voting, Answer answer) throws Exception {

        List<Answer> answers = findByVoting(voting);
        List<String> userList = new ArrayList<>();

        for (Answer answerIterator : answers){
            List<String> votes = getVoteListByAnswer(answerIterator);
            for (String voteIterator : votes){
                if (!userList.contains(user.getId().toString())) {
                    userList.add(voteIterator);
                }
            }
        }

        /*var signatureService2 = new SignatureService("C:\\Users\\aleks\\IdeaProjects\\electio\\ca.p12", "1234", "Test CA");
        Signature dsa = Signature.getInstance("SHA256withRSA");
        var vote2 = findByAnswer(answer).get(0);
        byte[] signature = vote2.getSignature();

        var cert1 = signatureService2.getCertificate(user.getKeyAlias());
        dsa.initVerify(cert1);
        dsa.update(vote2.getXml().getBytes());
        boolean verifies = dsa.verify(signature);
        System.out.println("111111111 Signature is ok: " + verifies);

        signatureService2.createCertificate("1234", "tyjtyjt", "rhrthr", 30, "1234");
        var cert2 = signatureService2.getCertificate("1234");
        dsa.initVerify(cert2);
        dsa.update(vote2.getXml().getBytes());
        boolean ver = dsa.verify(signature);
        System.out.println("22222222222222 Signature is ok: " + ver);*/

        if (!userList.contains(user.getId().toString())){

            var xml = signatureService.createXMLDocument(user, answer);
            var signatureData = signatureService.sign(xml.getBytes(), signatureService.getPrivateKey(user.getKeyAlias(), user.getPassword()),
                    signatureService.getCertificate(user.getKeyAlias()));

            Vote vote = new Vote();
            vote.setXml(new String(signatureData.get("XML")));
            vote.setSignature(signatureData.get("Signature"));
            voteRepo.save(vote);
        }
    }

    public void createResult(User user, Meeting meeting, Voting voting) throws Exception {
        var results = resultRepo.findAll();
        var resultExist = false;

        for (Result result : results){
            var xml = signatureService.convertToXML(result.getXml());
            if (Long.parseLong(xml.getElementsByTagName("Voting").item(0).getTextContent())==voting.getId()){
                resultExist = true;
            }
        }

        if (!resultExist){
            var answersVoting = getAnswers(voting);

            int numVoters = 0;
            for (Map.Entry<Answer, Integer> entry : answersVoting.entrySet()) {
                numVoters += entry.getValue();
            }

            var xml = signatureService.createResultXML(voting, numVoters, answersVoting, voting.getCreator());
            var signatureData = signatureService.sign(xml.getBytes(), signatureService.getPrivateKey(user.getKeyAlias(), user.getPassword()),
                    signatureService.getCertificate(user.getKeyAlias()));

            Result result = new Result();
            result.setXml(new String(signatureData.get("XML")));
            result.setSignature(signatureData.get("Signature"));
            resultRepo.save(result);
        }
    }

    public Map<String, String> getResult(Voting voting){
        voteMap.clear();

        var results = resultRepo.findAll();

        Map<String, String> resultMap = new LinkedHashMap<>();

        for (Result result : results){
            var xml = signatureService.convertToXML(result.getXml());
            if (Long.parseLong(xml.getElementsByTagName("Voting").item(0).getTextContent())==voting.getId()){
                resultMap.put("ProtocolNum", result.getId().toString());
                resultMap.put("Voting", voting.getId().toString());
                resultMap.put("NumVoters", xml.getElementsByTagName("NumVoters").item(0).getTextContent());
                NodeList voteNodeList = xml.getElementsByTagName("Vote").item(0).getChildNodes();
                for (int i = 0; i < voteNodeList.getLength(); i++) {
                    Node vote = voteNodeList.item(i);
                    if (vote.getNodeType() != Node.TEXT_NODE) {
                        NodeList voteProps = vote.getChildNodes();

                        String answerName = null;
                        String answerVotes = null;

                        for(int j = 0; j < voteProps.getLength(); j++) {
                            Node voteProp = voteProps.item(j);
                            if (voteProp.getNodeType() != Node.TEXT_NODE) {
                                if (voteProp.getNodeName().equals("AnswerName")){
                                    answerName = voteProp.getChildNodes().item(0).getTextContent();
                                } else if (voteProp.getNodeName().equals("NumVote")){
                                    answerVotes = voteProp.getChildNodes().item(0).getTextContent();
                                }
                            }
                        }
                        if (answerName != null || answerVotes != null){
                            voteMap.put(answerName, answerVotes);
                        }
                    }
                }

                resultMap.put("Chairperson", xml.getElementsByTagName("Chairperson").item(0).getTextContent());
            }
        }
        return resultMap;
    }

    public Map<String, String> getVoteMap(){
        return voteMap;
    }

    public List<String> getVoteListByAnswer(Answer answer) throws Exception {
        var votes = voteRepo.findAll();

        List<String> users = new ArrayList<>();

        for (Vote vote : votes){

            var xml = signatureService.convertToXML(vote.getXml());
            if (signatureService.checkSignature(signatureService, vote.getXml().getBytes(), vote.getSignature(),
                    userRepo.findById(Long.parseLong(xml.getElementsByTagName("User").item(0).getTextContent())).get())) {
                if (xml.getElementsByTagName("Answer").item(0).getTextContent().equals(answer.getId().toString())){
                    users.add(xml.getElementsByTagName("User").item(0).getTextContent());
                }
            }

        }

        return users;
    }

    public Map<Answer, Integer> getAnswers(Voting voting) throws Exception {
        Map<Answer, Integer> answers = new LinkedHashMap<>();
        var answerList = findByVoting(voting);
        for (Answer answer : answerList){
            List<String> votes = getVoteListByAnswer(answer);
            answers.put(answer, votes.size());
        }
        return answers;
    }

    public Optional<Voting> findVotingById(Long id){
        return votingRepo.findById(id);
    }
    public List<Meeting> findAllMeeting(){
        return meetingRepo.findAll();
    }
    public List<Voting> findByMeeting(Meeting meeting) {
        return votingRepo.findByMeeting(meeting);
    }
    public List<Answer> findByVoting(Voting voting) {
        return answerRepo.findByVoting(voting);
    }
}
