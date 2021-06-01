package com.rovoq.electio.service.signature;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.*;

import com.rovoq.electio.domain.Answer;
import com.rovoq.electio.domain.User;
import com.rovoq.electio.domain.Voting;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.crypto.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


/** The test CA can e.g. be created with             C:\tmp
 *
 * echo -e "AT\nUpper Austria\nSteyr\nMy Organization\nNetwork tests\nTest CA certificate\nme@myserver.com\n\n\n" | \
 openssl req -new -x509 -outform PEM -newkey rsa:2048 -nodes -keyout C:/tmp/ca.key -keyform PEM -out C:/tmp/ca.crt -days 365;
 echo "test password" |
 openssl pkcs12 -export -in C:/tmp/ca.crt -inkey C:/tmp/ca.key -out ca.p12 -name "Test CA" -passout stdin
 *
 * The created certificate can be displayed with
 *
 * openssl pkcs12 -nodes -info -in ca.p12 > C:/tmp/test.cert && openssl x509 -noout -text -in C:/tmp/test.cert
 */
//    newKeyStoreName test
//        var certificateGenerator = new X509CertificateGenerator("C:\\Users\\aleks\\IdeaProjects\\electio\\ca.p12", "1234", "Test CA", false);
//        certificateGenerator.createCertificate("alias1", "Test CN", 30, "test");
//        certificateGenerator.createCertificate("alias2" , "Adam", 30, "Adam");
public class SignatureService {

    private X509Certificate caCert;

    private RSAPrivateCrtKeyParameters caPrivateKey;

    public SignatureService(String caFile, String caPassword, String caAlias) throws Exception{
        System.out.println("Loading CA certificate and private key from file '" + caFile + "', using alias '" + caAlias);
        KeyStore caKs = KeyStore.getInstance("PKCS12");
        caKs.load(new FileInputStream(caFile), caPassword.toCharArray());

        // load the key entry from the keystore
        Key key = caKs.getKey(caAlias, caPassword.toCharArray());
        if (key == null) {
            throw new RuntimeException("Got null key from keystore!");
        }
        RSAPrivateCrtKey privKey = (RSAPrivateCrtKey) key;
        caPrivateKey = new RSAPrivateCrtKeyParameters(privKey.getModulus(), privKey.getPublicExponent(), privKey.getPrivateExponent(),
                privKey.getPrimeP(), privKey.getPrimeQ(), privKey.getPrimeExponentP(), privKey.getPrimeExponentQ(), privKey.getCrtCoefficient());
        // and get the certificate
        caCert = (X509Certificate) caKs.getCertificate(caAlias);
        if (caCert == null) {
            throw new RuntimeException("Got null cert from keystore!");
        }
        System.out.println("Successfully loaded CA key and certificate. CA DN is '" + caCert.getSubjectDN().getName() + "'");
        caCert.verify(caCert.getPublicKey());
        System.out.println("Successfully verified CA certificate with its own public key.");
    }

    public void createCertificate(String alias, String dn, String emailAdress, int validityDays, String exportPassword) throws Exception{
        var checkCertificate = (X509Certificate) getCertificate(alias);

        if (checkCertificate == null){
            System.out.println("Generating certificate for distinguished subject name '" +
                    dn + "', valid for " + validityDays + " days");
            SecureRandom sr = new SecureRandom();

            PublicKey pubKey;
            PrivateKey privKey;

            System.out.println("Creating RSA keypair");
            // generate the keypair for the new certificate
            // this is the JSSE way of key generation
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024, sr);
            KeyPair keypair = keyGen.generateKeyPair();
            privKey = keypair.getPrivate();
            pubKey = keypair.getPublic();

            Calendar expiry = Calendar.getInstance();
            expiry.add(Calendar.DAY_OF_YEAR, validityDays);

            X509Name x509Name = new X509Name("CN=" + dn + ", EMAILADDRESS=" + emailAdress);

            V3TBSCertificateGenerator certGen = new V3TBSCertificateGenerator();
            certGen.setSerialNumber(new DERInteger(BigInteger.valueOf(System.currentTimeMillis())));
            certGen.setIssuer(PrincipalUtil.getSubjectX509Principal(caCert));
            certGen.setSubject(x509Name);
            var sigOID = X509Util.getAlgorithmOID("SHA1WithRSAEncryption");
            AlgorithmIdentifier sigAlgId = new AlgorithmIdentifier(sigOID, new DERNull());
            certGen.setSignature(sigAlgId);
            certGen.setSubjectPublicKeyInfo(new SubjectPublicKeyInfo((ASN1Sequence)new ASN1InputStream(
                    new ByteArrayInputStream(pubKey.getEncoded())).readObject()));
            certGen.setStartDate(new Time(new Date(System.currentTimeMillis())));
            certGen.setEndDate(new Time(expiry.getTime()));

            System.out.println("Certificate structure generated, creating SHA1 digest");
            // attention: hard coded to be SHA1+RSA!
            SHA1Digest digester = new SHA1Digest();
            AsymmetricBlockCipher rsa = new PKCS1Encoding(new RSAEngine());
            var tbsCert = certGen.generateTBSCertificate();

            ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
            DEROutputStream         dOut = new DEROutputStream(bOut);
            dOut.writeObject(tbsCert);

            // and now sign
            byte[] signature;

            // or the JCE way
            PrivateKey caPrivKey = KeyFactory.getInstance("RSA").generatePrivate(
                    new RSAPrivateCrtKeySpec(caPrivateKey.getModulus(), caPrivateKey.getPublicExponent(),
                            caPrivateKey.getExponent(), caPrivateKey.getP(), caPrivateKey.getQ(),
                            caPrivateKey.getDP(), caPrivateKey.getDQ(), caPrivateKey.getQInv()));

            Signature sig = Signature.getInstance(sigOID.getId());
            sig.initSign(caPrivKey, sr);
            sig.update(bOut.toByteArray());
            signature = sig.sign();

            System.out.println("SHA1/RSA signature of digest is '" + new String(Hex.encodeHex(signature)) + "'");

            // and finally construct the certificate structure
            ASN1EncodableVector  v = new ASN1EncodableVector();

            v.add(tbsCert);
            v.add(sigAlgId);
            v.add(new DERBitString(signature));

            Certificate certificate = Certificate.getInstance(new DERSequence(v));
            var clientCert = new X509CertificateObject(certificate);
            System.out.println("Verifying certificate for correct signature with CA public key");
            clientCert.verify(caCert.getPublicKey());

            // and export as PKCS12 formatted file along with the private key and the CA certificate
            System.out.println("Exporting certificate in PKCS12 format");

            KeyStore store = KeyStore.getInstance("JCEKS");

            char[] password = "test".toCharArray();
            String path = "C:\\Users\\aleks\\IdeaProjects\\electio\\newKeyStoreName";
            java.io.FileInputStream fis = new FileInputStream(path);

            store.load(fis, password);

            X509Certificate[] chain = new X509Certificate[2];
            // first the client, then the CA certificate
            chain[0] = clientCert;
            chain[1] = caCert;

            store.setKeyEntry(alias, privKey, exportPassword.toCharArray(), chain);

            java.io.FileOutputStream fos = null;
            fos = new java.io.FileOutputStream("newKeyStoreName");
            store.store(fos, password);
            System.out.println("data stored");
        }
    }

    public Key getPrivateKey(String alias, String password) throws Exception{
        KeyStore store = KeyStore.getInstance("JCEKS");

        char[] pswd = "test".toCharArray();
        String path = "C:\\Users\\aleks\\IdeaProjects\\electio\\newKeyStoreName";
        java.io.FileInputStream fis = new FileInputStream(path);

        store.load(fis, pswd);

        return store.getKey(alias, password.toCharArray());
    }

    public java.security.cert.Certificate getCertificate(String alias) throws Exception{
        KeyStore store = KeyStore.getInstance("JCEKS");

        char[] password = "test".toCharArray();
        String path = "C:\\Users\\aleks\\IdeaProjects\\electio\\newKeyStoreName";
        java.io.FileInputStream fis = new FileInputStream(path);

        store.load(fis, password);

//        (X509Certificate) store.getCertificate(alias);
        return store.getCertificate(alias);
    }

    public HashMap<String, byte[]> sign(byte[] data, Key privateKey, java.security.cert.Certificate certificate)throws Exception{
        // Подпись
        // Digital Signature
        Signature dsa = Signature.getInstance("SHA256withRSA");
        dsa.initSign((PrivateKey) privateKey);
        // Update and sign the data
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, certificate);
//        byte[] encryptData = cipher.doFinal(data);
        byte[] encryptData = data;
        dsa.update(encryptData);
        byte[] signature = dsa.sign();

        // Проверка подписи
        dsa.initVerify(certificate);
        dsa.update(encryptData);
        boolean verifies = dsa.verify(signature);
        System.out.println("Signature is ok: " + verifies);

        HashMap<String, byte[]> signatureData = new HashMap<>();
//        List<byte[]> signatureData = new ArrayList<>();
        signatureData.put("XML", encryptData);
        signatureData.put("Signature", signature);
        return signatureData;

        // Decrypt if signature is correct
//        if (verifies) {
//            cipher.init(Cipher.DECRYPT_MODE, privateKey);
//            byte[] result = cipher.doFinal(encryptData);
//        }
    }

    public boolean checkSignature(SignatureService signatureService, byte[] data, byte[] signature, User user) throws Exception {
        Signature dsa = Signature.getInstance("SHA256withRSA");
//        var vote2 = findByAnswer(answer).get(0);
//        byte[] signature = vote2.getSignature();

        var cert = signatureService.getCertificate(user.getKeyAlias());
        dsa.initVerify(cert);
        dsa.update(data);
        boolean verifies = dsa.verify(signature);
        return verifies;
    }

    private  final  String  FILE_data = "data.xml";
    public String createXMLDocument(User user, Answer completeAnswer){
        DocumentBuilderFactory dbf = null;
        DocumentBuilder db  = null;
        Document doc = null;
        try {
            dbf = DocumentBuilderFactory.newInstance();
            db  = dbf.newDocumentBuilder();
            doc = db.newDocument();

            Element e_root   = doc.createElement("Vote");

            Element el = doc.createElement("User");
            el.setTextContent(user.getId().toString());
            e_root.appendChild (el);

            el = doc.createElement("Answer");
            el.setTextContent(completeAnswer.getId().toString());
            e_root.appendChild (el);

            doc.appendChild(e_root);

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } finally {
            // Сохраняем Document в XML-файл
            if (doc != null){
//                writeDocument(doc, FILE_data);
                return convertToString(doc);
            }
        }
        return null;
    }

    public String createResultXML(Voting voting, int numVoters, Map<Answer, Integer> answers, User user){
        DocumentBuilderFactory dbf = null;
        DocumentBuilder db  = null;
        Document doc = null;
        try {
            dbf = DocumentBuilderFactory.newInstance();
            db  = dbf.newDocumentBuilder();
            doc = db.newDocument();

            Element e_root   = doc.createElement("Result");

            Element el = doc.createElement("Voting");
            el.setTextContent(voting.getId().toString());
            e_root.appendChild (el);

            el = doc.createElement("NumVoters");
            el.setTextContent(String.valueOf(numVoters));
            e_root.appendChild (el);

            var vote = doc.createElement("Vote");
            e_root.appendChild (vote);

            for (Map.Entry<Answer, Integer> entry : answers.entrySet()) {
                Element answerEl = doc.createElement("Answer");
                vote.appendChild(answerEl);

                Element elInner = doc.createElement("AnswerName");
                elInner.setTextContent(entry.getKey().getName());
                answerEl.appendChild(elInner);

                elInner = doc.createElement("NumVote");
                elInner.setTextContent(entry.getValue().toString());
                answerEl.appendChild(elInner);
            }

            el = doc.createElement("Chairperson");
            el.setTextContent(String.valueOf(user.getId()));
            e_root.appendChild (el);

            doc.appendChild(e_root);

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } finally {
            // Сохраняем Document в XML-файл
            if (doc != null){
//                writeDocument(doc, FILE_data);
                return convertToString(doc);
            }
        }
        return null;
    }

    public String convertToString(Document doc) {
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error converting to String", ex);
        }
    }

    public Document convertToXML(String xmlString){
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xmlString)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void writeDocument(Document document, String path)
            throws TransformerFactoryConfigurationError
    {
        Transformer trf = null;
        DOMSource src = null;
        FileOutputStream fos = null;
        try {
            trf = TransformerFactory.newInstance()
                    .newTransformer();
            src = new DOMSource(document);
            fos = new FileOutputStream(path);

            StreamResult result = new StreamResult(fos);
            trf.transform(src, result);
        } catch (TransformerException e) {
            e.printStackTrace(System.out);
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }
}
