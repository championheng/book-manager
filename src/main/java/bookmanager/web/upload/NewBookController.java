package bookmanager.web.upload;

import bookmanager.dao.dbservice.BookInfoService;
import bookmanager.dao.dbservice.BookLabelService;
import bookmanager.dao.dbservice.BookRelationLabelService;
import bookmanager.model.po.BookInfoPO;
import bookmanager.model.po.BookRelationLabelPO;
import bookmanager.utilclass.COSStorage;
import bookmanager.utilclass.DateToString;
import bookmanager.utilclass.Tools;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.*;

/**
 * @Author: spider_hgyi
 * @Date: Created in 下午8:14 17-12-3.
 * @Modified By:
 * @Description:
 */
@WebServlet(urlPatterns = "/upload")
@MultipartConfig
public class NewBookController extends HttpServlet {
    private BookInfoService bookInfoService;
    private BookLabelService bookLabelService;
    private BookRelationLabelService bookRelationLabelService;
    private COSStorage cosStorage;

    // 手动获取bean
    public void init() throws ServletException {
        ServletContext servletContext = this.getServletContext();
        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        bookInfoService = (BookInfoService) ctx.getBean("bookInfoServiceImpl");
        bookLabelService = (BookLabelService) ctx.getBean("bookLabelServiceImpl");
        bookRelationLabelService = (BookRelationLabelService) ctx.getBean("bookRelationLabelServiceImpl");
        cosStorage = (COSStorage) ctx.getBean("cosStorage");
    }

    /**
     * @param:
     * @return:
     * @description: 上传成功跳转至我的书籍页面，上传失败回填表单
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher(request.getContextPath() + "/WEB-INF/view/pushbook.jsp").forward(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("utf8");
        response.setCharacterEncoding("utf8");

        String bookName = request.getParameter("bookName");
        String author = request.getParameter("author");
//        int uid = (Integer) request.getSession(false).getAttribute("uid");
        int amount = Integer.parseInt(request.getParameter("amount"));
        String string = request.getParameter("types");
        String date = DateToString.getStringDate();
        String describ = request.getParameter("describ");

        BookInfoPO bookInfoPO = new BookInfoPO();
        Part bookPicture;
        String bookPictureName = "";

        // 处理图片
        if ((bookPicture = request.getPart("bookPicture")).getSize() != 0) {
            String uuid = UUID.randomUUID().toString();
            bookPictureName = "http://bookmanager-1253675585.coscd.myqcloud.com/" + uuid;

            try {
                cosStorage.uploadPicture(uuid, bookPicture.getInputStream(), bookPicture.getSize());
            } catch (IOException e) {
                System.out.println("cos上传图片出错");
                e.printStackTrace();
            }

            cosStorage.shutdownClient();
        }

        // 保存上传图片信息
        bookInfoPO.setUgkName(bookName);
        bookInfoPO.setAuthor(author);
//        bookInfoPO.setUgkUid(uid);
        bookInfoPO.setUgkUid(1);
        bookInfoPO.setAmount(amount);
        bookInfoPO.setUploadDate(date);
        bookInfoPO.setBookPicture(bookPictureName);
        bookInfoPO.setDescrib(describ);

        try {
            bookInfoService.save(bookInfoPO);
        } catch (DuplicateKeyException e) {
            // 数据库中已经有用户要上传的信息，提醒用户应该在我的书籍页面进行信息的修改
            e.printStackTrace();
            return;
        }

        // 处理书籍分类
        Tools tools = new Tools();
        Set<String> types = tools.getTypes(string);
        for (String type : types) {
            System.out.println(type);
        }

        // 得到新增书籍的ID
        int bookId = bookInfoService.getBookIDByBookNameAndUID(bookName, 1);
        // 查询所属分类的ID 并 save 至 book_relation_label表
        for (String type : types) {
            int labelId = bookLabelService.getPkIdByName(type);
            BookRelationLabelPO bookRelationLabelPO = new BookRelationLabelPO(bookId, labelId);
            bookRelationLabelService.save(bookRelationLabelPO);
        }

        response.sendRedirect("mybook");
    }
}
