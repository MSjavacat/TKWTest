package com.ms.seckill.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.seckill.pojo.User;
import com.ms.seckill.vo.RespBean;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author MS
 * @create 2022-09-02-15:50
 */
public class UserUtil {
    private static void createUser(int count) throws Exception{
        List<User> users = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            User user = new User();
            user.setId(13000000000L + i);
            user.setLoginCount(1);
            user.setNickname("user" + i);
            user.setRegisterDate(new Date());
            user.setSalt("1a2b3c4d");
            user.setPassword(MD5Util.inputPassToDBPass("123456",user.getSalt()));
            users.add(user);
        }

        System.out.println("create user");
        // 插入数据库
        Connection conn = getConn();
        String sql =  "insert into t_user(login_count, nickname, register_date, salt, password, id)values(?,?,?,?,?,?)";
        PreparedStatement ps = conn.prepareStatement(sql);
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            ps.setInt(1, user.getLoginCount());
            ps.setString(2, user.getNickname());
            ps.setTimestamp(3, new Timestamp(user.getRegisterDate().getTime()));
            ps.setString(4, user.getSalt());
            ps.setString(5, user.getPassword());
            ps.setLong(6, user.getId());
            ps.addBatch();
        }
        ps.executeBatch();
        ps.close();
        conn.close();
        System.out.println("insert into db");

        // 登录，生成userTicket
        String urlString =  "http://localhost:8080/login/doLogin";
        File file = new File("C:\\Users\\ZWQ\\Desktop\\config.txt");
        if (file.exists()) {
            file.delete();
        }
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.seek(0);
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            // 发起请求
            URL url = new URL(urlString);
            HttpURLConnection cn = (HttpURLConnection) url.openConnection();
            cn.setRequestMethod("POST");
            cn.setDoOutput(true);
            OutputStream outputStream = cn.getOutputStream();
            String params = "mobile=" + user.getId() + "&password=" + MD5Util.inputPassToFromPass("123456");
            outputStream.write(params.getBytes());
            outputStream.flush();
            // 获取结果
            InputStream inputStream = cn.getInputStream();
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            byte[] buff = new byte[1024];
            int len = 0;
            while ((len = inputStream.read(buff)) >= 0) {
                bout.write(buff,0,len);
            }
            inputStream.close();
            bout.close();
            String response = new String(bout.toByteArray());
            ObjectMapper mapper = new ObjectMapper();
            RespBean respBean = mapper.readValue(response, RespBean.class);
            String userTicket = (String) respBean.getObj();
            System.out.println("create userTicket:" + user.getId());
            String row = user.getId() + "," + userTicket;
            raf.seek(raf.length());
            raf.write(row.getBytes());
            raf.write("\r\n".getBytes());
            System.out.println("write to file:" + user.getId());
        }
        raf.close();
        System.out.println("over");
    }

    /**
     * 获取数据库连接
     * @return
     * @throws Exception
     */
    private static Connection getConn() throws Exception{
        String url = "jdbc:mysql://localhost:3306/seckill?useUnicode=true&characterEncoding=UTF-8&serverTimeZone=Asia/shanghai";
        String username = "root";
        String password = "root";
        String driver = "com.mysql.cj.jdbc.Driver";
        Class.forName(driver);
        return DriverManager.getConnection(url,username,password);
    }

    public static void main(String[] args) throws Exception {
        createUser(5000);
    }
}
