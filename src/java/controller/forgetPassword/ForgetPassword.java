package controller.forgetPassword;

import dal.AccountDBContext;
import dal.UserDBContext;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Properties;
import java.util.Random;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import model.Account;
import model.User;

@WebServlet("/forgetPassword/forget")
public class ForgetPassword extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("./forgetPassword.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String parentEmail = request.getParameter("parentEmail");
        HttpSession session = request.getSession();

        if (username == null || parentEmail == null) {
            setErrorAndForward(request, response, "Tên tài khoản hoặc email không đúng, vui lòng nhập lại!");
            return;
        }

        Account account = getAccountByUsername(username);
        if (account == null) {
            setErrorAndForward(request, response, "Tên tài khoản hoặc email không đúng, vui lòng nhập lại!");
            return;
        }

        User user = getUserByUsername(username);
        if (user == null || !parentEmail.equals(user.getParent_email())) {
            setErrorAndForward(request, response, "Tên tài khoản hoặc email không đúng, vui lòng nhập lại!");
            return;
        }

        // Generate a new password
        String newPassword = generateRandomPassword(8);

        // Update the password in the database
        updatePassword(username, newPassword);

        // Send the new password via email
        if (!sendEmail(parentEmail, newPassword)) {
            setErrorAndForward(request, response, "Failed to send email. Please try again.");
            return;
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("../forgetPassword/confirmEmail.jsp");
        request.setAttribute("message", "Mật khẩu mới đã được gửi đến bạn, vui lòng kiểm tra email.");
        session.setAttribute("passGen", newPassword);
         session.setAttribute("accountForgetPass", account);
        session.setMaxInactiveInterval(300);
        session.setAttribute("email", parentEmail);
        dispatcher.forward(request, response);
    }

    private Account getAccountByUsername(String username) {
        AccountDBContext accountDB = new AccountDBContext();
        try {
            return accountDB.checkAccountExisted(username);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private User getUserByUsername(String username) {
        UserDBContext userDB = new UserDBContext();
        try {
            return userDB.getUserByUsername(username);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void updatePassword(String username, String newPassword) {
        AccountDBContext accountDB = new AccountDBContext();
        accountDB.updatePassword(username, newPassword);
    }

    private boolean sendEmail(String to, String newPassword) {
        String from = "nguyenpthe172112@fpt.edu.vn";
        String host = "smtp.gmail.com";
        String port = "465";
        String authPassword = "lvku svvd feas xryh"; // Use a secure method to store this

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.socketFactory.port", port);
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", port);

        Session emailSession = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, authPassword);
            }
        });

        try {
            MimeMessage message = new MimeMessage(emailSession);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject("Hello guys!");
            message.setText("This is your verify code: " + newPassword);

            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void setErrorAndForward(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        request.setAttribute("mess", message);
        request.getRequestDispatcher("./forgetPassword.jsp").forward(request, response);
    }

    private String generateRandomPassword(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random rand = new Random();
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            password.append(characters.charAt(rand.nextInt(characters.length())));
        }
        return password.toString();
    }

    @Override
    public String getServletInfo() {
        return "Forget Password Servlet";
    }
}
