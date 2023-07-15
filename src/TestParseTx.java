

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Consts;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

public class TestParseTx {
    public static JSONObject ans = new JSONObject();
    public static int[] age = new int[10];
    // index 0 => 0~18
    // index 1 => 18~25
    // index 2 => 25~30
    // index 3 => 30~35
    // index 4 => 35~40
    // index 5 => 40~45
    // index 6 => 45~
    public static int[] work_year_inf = new int[10];
    // index 0 => 0~1
    // index 1 => 1~2
    // index 2 => 2~3
    public static int[] degree = new int[6];
    // 1 专科
    // 2 本科
    // 3 硕士
    // 4 博士
    // 5 博士后
    public static int[] college_type = new int[8];
    public static String calcAuthorization(String source, String secretId, String secretKey, String datetime)
            throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
        String signStr = "x-date: " + datetime + "\n" + "x-source: " + source;
        Mac mac = Mac.getInstance("HmacSHA1");
        Key sKey = new SecretKeySpec(secretKey.getBytes("UTF-8"), mac.getAlgorithm());
        mac.init(sKey);
        byte[] hash = mac.doFinal(signStr.getBytes("UTF-8"));
        String sig = new String(Base64.encodeBase64(hash), Consts.UTF_8);

        String auth = "hmac id=\"" + secretId + "\", algorithm=\"hmac-sha1\", headers=\"x-date x-source\", signature=\"" + sig + "\"";
        return auth;
    }

    public static void testResumeParser(String url, String fileName, String secretId, String secretKey) throws Exception {
        // 读取简历内容
        byte[] bytes = org.apache.commons.io.FileUtils.readFileToByteArray(new File(fileName));
        String data = new String(Base64.encodeBase64(bytes), Consts.UTF_8);
        System.out.println("data len: " + data.length());

        // 设置请求信息
        JSONObject json = new JSONObject();
        json.put("file_name", fileName);	// 文件名
        json.put("file_cont", data);	// 经base64编码过的文件内容
        json.put("ocr_type", 1);		// 设置ocr_type类型
        StringEntity params = new StringEntity(json.toString(), Consts.UTF_8);
        System.out.println("data len: " + params.getContentLength());

        Calendar cd = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String datetime = sdf.format(cd.getTime());

        String source = "market";
        String auth = calcAuthorization(source, secretId, secretKey, datetime);

        // 发送请求并处理json结果
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);

            // 设置头字段
            httpPost.addHeader("X-Source", "market");
            httpPost.addHeader("X-Date", datetime);
            httpPost.addHeader("Authorization", auth);
            httpPost.addHeader("content-type", "application/json");
            httpPost.setEntity(params);

            try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
                int httpCode = response.getStatusLine().getStatusCode();
                System.out.println("http code: " + httpCode);

                // 处理返回结果
                if (httpCode >= 200 && httpCode < 300) {
                    String resCont = EntityUtils.toString(response.getEntity(), Consts.UTF_8);
                    JSONObject res = new JSONObject(resCont);
                    System.out.println(res.getJSONObject("result"));
                    
                    File tmp = new File(fileName);
                    ans.put(tmp.getName(),res.getJSONObject("result"));
                    
                    res = res.getJSONObject("result");
                    if(res.has("age")) {
                        int dispose = Integer.parseInt(res.get("age").toString());
                        if(dispose < 18) 
                            age[0]++;
                        else if(dispose < 25)
                            age[1]++;   
                        else if(dispose < 30)
                            age[2]++;
                        else if(dispose < 35)
                            age[3]++;
                        else if(dispose < 40)
                            age[4]++;
                        else if(dispose < 45)
                            age[5]++;
                        else
                            age[6]++; 
                    }
                    if(res.has("work_year_norm")) {
                        double dispose1 = Double.parseDouble(res.get("work_year_norm").toString());
                        if (dispose1 == 0)
                            work_year_inf[0]++;
                        else if (dispose1 < 3)
                            work_year_inf[1]++;
                        else if (dispose1 < 5)
                            work_year_inf[2]++;
                        else if (dispose1 < 8)
                            work_year_inf[3]++;
                        else if (dispose1 < 10)
                            work_year_inf[4]++;
                        else if (dispose1 < 15)
                            work_year_inf[5]++;
                        else 
                            work_year_inf[6]++;
                    }
                    
                    if(res.has("degree")) {
                        String dispose2 = res.get("degree").toString();
                        switch (dispose2) {
                            case "本科" -> degree[1]++;
                            case "硕士" -> degree[2]++;
                            case "博士" -> degree[3]++;
                            case "博士后" -> degree[4]++;
                            case "专科" -> degree[0]++;
                        }
                    }
                    
                    if(res.has("college_type")) {
                        int dispose3 = Integer.parseInt(res.get("college_type").toString());
                        college_type[dispose3]++;
                    }
                    
                    res = new JSONObject(resCont);
                    
                    JSONObject status = res.getJSONObject("status");
                    if(status.getInt("code") != 200) {
                        System.out.println("request failed: code=<" + status.getInt("code") + ">, message=<" + status.getString("message") + ">");
                    }
                    else {
                        JSONObject result = res.getJSONObject("result");
                        System.out.println("result:\n" + result.toString(4));
                        System.out.println("succeeded");
                    }
                }
                else {
                    System.out.println("Unexpected http code: " + httpCode);
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    public static void getAllFile(File fileInput, List<File> allFileList) {
        // 获取文件列表
        File[] fileList = fileInput.listFiles();
        assert fileList != null;
        for (File file : fileList) {
            if (file.isDirectory()) {
                System.out.println("嵌套文件夹,默认递归处理");
                getAllFile(file, allFileList);
            } else {
                // 如果是文件则将其加入到文件数组中
                allFileList.add(file);
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        String url = "https://service-9wsy8usn-1302482110.bj.apigw.tencentcs.com/release/ResumeParser";		//腾讯云ResumeSDK简历解析接口服务url
//        String secretId = "AKID9RE3mnhg36tuouVkM4oIxj34Con4lpkZJA48";	//云市场分配的密钥Id
//        String secretKey = "4xkClvyWsB8p8N5e5em44XMChNY3WZ5FgcC6P104";	//云市场分配的密钥Key


        String secretId = "AKIDNfSzf2RGbMISgMU1Dk1GSk54s5v2i6mribQW";	//云市场分配的密钥Id
        String secretKey = "8SZOnoop3P7z9uEDmP3o4w3INdWP096vtFXEUfdG";	//云市场分配的密钥Key
        List<File> allFileList = new ArrayList<>();
        String basePath = "D:\\backend\\backend\\untitled\\src\\data"; // 这里填需要遍历的路径
        File dir = new File(basePath);
        int i = 0;
        getAllFile(dir, allFileList);
        for (File file : allFileList) {
            System.out.println(file.getAbsolutePath());
            testResumeParser(url, file.getAbsolutePath(), secretId, secretKey);
            i++;
            if(i == 100) {
                secretId  =  "AKIDIf4ish54pG8jCvWTKOi2zgD33aPH0iBiL8K0";
                secretKey =  "5940IJOAPddmtYS0B27q7x4QzB8BHv2W3470Gq0K";
            }
        }
        File file = new File("D:\\backend\\backend\\untitled\\src\\ans.json");
        try {
            if (file.exists()) {
                if (file.delete())
                    System.out.println("成功删除旧文件");
            }
            if(file.createNewFile())
                System.out.println("成功创建文件");

            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write(ans.toString(4));
            
            bw.flush();
            bw.close();
            fw.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        file = new File("D:\\backend\\backend\\untitled\\src\\mes.txt");
        try {
            if (file.exists()) {
                if (file.delete())
                    System.out.println("成功删除旧文件");
            }
            if(file.createNewFile())
                System.out.println("成功创建文件");

            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write("年龄小于18岁: " + age[0] + "\n");
            bw.write("18岁到25岁: " + age[1] + "\n");
            bw.write("25岁到30岁: " + age[2] + "\n");
            bw.write("30岁到35岁: " + age[3] + "\n");
            bw.write("35岁到40岁: " + age[4] + "\n");
            bw.write("40岁到45岁: " + age[5] + "\n");
            
            bw.write("=========\n");
            
            bw.write("应届生: " + work_year_inf[0] + "\n");
            bw.write("三年以内: " + work_year_inf[1] + "\n");
            bw.write("三到五年: " + work_year_inf[2] + "\n");
            bw.write("五到八年: " + work_year_inf[3] + "\n");
            bw.write("八到十年: " + work_year_inf[3] + "\n");
            bw.write("十到十五年: " + work_year_inf[3] + "\n");
            bw.write("十五年以上: " + work_year_inf[3] + "\n");
            
            bw.write("=========\n");
            
            bw.write("专科: " + degree[0] + "\n");
            bw.write("本科: " + degree[1] + "\n");
            bw.write("硕士: " + degree[2] + "\n");
            bw.write("博士: " + degree[3] + "\n");
            bw.write("博士后: " + degree[4] + "\n");

            bw.write("=========\n");

            bw.write("普通院校: " + college_type[0] + "\n");
            bw.write("985院校: " + college_type[1] + "\n");
            bw.write("211院校: " + college_type[2] + "\n");
            bw.write("港澳台院校: " + college_type[3] + "\n");
            bw.write("海外院校: " + college_type[4] + "\n");
            bw.write("中学: " + college_type[5] + "\n");
            bw.write("职业教育: " + college_type[6] + "\n");
            bw.write("培训机构: " + college_type[7] + "\n");
            
            bw.flush();
            bw.close();
            fw.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
}

