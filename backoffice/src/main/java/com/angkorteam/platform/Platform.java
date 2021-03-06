package com.angkorteam.platform;

import com.angkorteam.framework.jdbc.InsertQuery;
import com.angkorteam.framework.jdbc.JoinType;
import com.angkorteam.framework.jdbc.SelectQuery;
import com.angkorteam.framework.spring.JdbcTemplate;
import com.angkorteam.framework.spring.NamedParameterJdbcTemplate;
import com.angkorteam.platform.bean.TransactionManager;
import com.angkorteam.platform.mobile.Links;
import com.angkorteam.platform.model.PlatformRole;
import com.angkorteam.platform.model.PlatformUser;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.configuration.XMLPropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.wicket.protocol.http.WebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by socheatkhauv on 27/1/17.
 */
public abstract class Platform {

    private static ServletContext servletContext;

    public static PlatformUser getCurrentUser(HttpServletRequest request) throws UnsupportedEncodingException {
        String authorization = request.getHeader("Authorization");
        if (!Strings.isNullOrEmpty(authorization)) {
            byte[] base64Token = authorization.substring(6).getBytes("UTF-8");
            byte[] decoded = Base64.decodeBase64(base64Token);
            String token = new String(decoded, "UTF-8");
            Integer delim = token.indexOf(":");
            String accessToken = token.substring(0, delim);
            JdbcTemplate jdbcTemplate = Platform.getBean(JdbcTemplate.class);
            return jdbcTemplate.queryForObject("select * from platform_user where access_token = ?", PlatformUser.class, accessToken);
        }
        return null;
    }

    public static Links buildLinks(HttpServletRequest request, long total, long limit) {
        String queryString = request.getQueryString();
        List<String> params = Lists.newArrayList();
        if (!Strings.isNullOrEmpty(queryString)) {
            String[] temps = StringUtils.split(queryString, '&');
            for (String temp : temps) {
                if (!StringUtils.startsWithIgnoreCase(temp, "offset=")) {
                    params.add(temp);
                }
            }
        }
        String url = request.getRequestURL().toString();
        long page = ServletRequestUtils.getLongParameter(request, "offset", 1L);
        if (page < 1) {
            page = 1;
        }
        Links linksVO = new Links();
        {
            // first page
            long currentPage = 1L;
            List<String> actions = new ArrayList<>(params);
            actions.add("offset=" + currentPage);
            linksVO.setFirst(url + "?" + StringUtils.join(actions, '&'));
        }
        {
            // last page
            long currentPage = 0L;
            if (total % limit == 0L) {
                currentPage = total / limit;
            } else {
                currentPage = (total / limit) + 1L;
            }
            List<String> actions = new ArrayList<>(params);
            actions.add("offset=" + currentPage);
            linksVO.setLast(url + "?" + StringUtils.join(actions, '&'));
        }
        {
            // current page
            long currentPage = page;
            List<String> actions = new ArrayList<>(params);
            actions.add("offset=" + currentPage);
            linksVO.setSelf(url + "?" + StringUtils.join(actions, '&'));
        }
        {
            // next page
            long currentPage = page;
            if (total % limit == 0) {
                if (currentPage < (total / limit)) {
                    currentPage = currentPage + 1;
                    List<String> actions = new ArrayList<>(params);
                    actions.add("offset=" + currentPage);
                    linksVO.setNext(url + "?" + StringUtils.join(actions, '&'));
                } else {
                    linksVO.setNext(null);
                }
            } else {
                if (currentPage <= (total / limit)) {
                    currentPage = currentPage + 1;
                    List<String> actions = new ArrayList<>(params);
                    actions.add("offset=" + currentPage);
                    linksVO.setNext(url + "?" + StringUtils.join(actions, '&'));
                } else {
                    linksVO.setNext(null);
                }
            }

        }
        {
            // previous page
            long currentPage = page;
            if (currentPage > 1) {
                currentPage = currentPage - 1;
                List<String> actions = new ArrayList<>(params);
                actions.add("offset=" + currentPage);
                linksVO.setPrevious(url + "?" + StringUtils.join(actions, '&'));
            } else {
                linksVO.setPrevious(null);
            }
        }
        return linksVO;
    }

    public static boolean hasAccess(HttpServletRequest request, Class<?> clazz) {

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Basic ")) {
            return false;
        }
        byte[] base64Token = null;
        try {
            base64Token = authorization.substring(6).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return false;
        }
        byte[] decoded;
        try {
            decoded = Base64.decodeBase64(base64Token);
        } catch (IllegalArgumentException e) {
            return false;
        }

        String token = null;
        try {
            token = new String(decoded, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return false;
        }
        int delim = token.indexOf(":");
        if (delim == -1) {
            return false;
        }
        String accessToken = token.substring(0, delim);

        JdbcTemplate jdbcTemplate = Platform.getBean(JdbcTemplate.class);
        PlatformUser userRecord = jdbcTemplate.queryForObject("select * from platform_user where access_token = ?", PlatformUser.class, accessToken);
        PlatformRole roleRecord = jdbcTemplate.queryForObject("select * from platform_role where platform_role_id = ?", PlatformRole.class, userRecord.getPlatformRoleId());

        SelectQuery selectQuery = new SelectQuery("platform_role");
        selectQuery.addField("platform_role.name");
        selectQuery.addJoin(JoinType.InnerJoin, "platform_rest_role", "platform_role.platform_role_id = platform_rest_role.platform_role_id");
        selectQuery.addJoin(JoinType.InnerJoin, "platform_rest", "platform_rest_role.platform_rest_id = platform_rest.platform_rest_id");
        selectQuery.addWhere("platform_rest.java_class = ?");
        List<String> serviceRoles = jdbcTemplate.queryForList(selectQuery.toSQL(), String.class, clazz.getName());
        if (serviceRoles == null || serviceRoles.isEmpty()) {
        } else {
            String userRole = roleRecord.getName();
            if (userRole == null || "".equals(userRole) || !serviceRoles.contains(userRole)) {
                return false;
            }
        }
        return true;
    }

    public static String getSetting(String key) {
        JdbcTemplate jdbcTemplate = Platform.getBean(JdbcTemplate.class);
        return jdbcTemplate.queryForObject("select value from platform_setting where `key` = ?", String.class, key);
    }

    public static void putSetting(String key, String value) {
        JdbcTemplate jdbcTemplate = Platform.getBean(JdbcTemplate.class);
        int count = jdbcTemplate.queryForObject("select count(*) from platform_setting where `key` = ?", int.class, key);
        if (count > 0) {
            jdbcTemplate.update("update platform_setting set value = ? where `key` = ?", value, key);
        } else {
            InsertQuery insertQuery = new InsertQuery("platform_setting");
            insertQuery.addValue("platform_setting_id = :setting_id", randomUUIDLong("platform_setting"));
            insertQuery.addValue("description = :description", "");
            insertQuery.addValue("version = :version", 1);
            insertQuery.addValue("`key` = :key", key);
            NamedParameterJdbcTemplate named = Platform.getBean(NamedParameterJdbcTemplate.class);
            named.update(insertQuery.toSQL(), insertQuery.getParam());
        }
    }

    public static Long randomUUIDLong(String tableName) {
        TransactionManager transactionManager = Platform.getBean(TransactionManager.class);
        JdbcTemplate jdbcTemplate = transactionManager.createJdbcTemplate();
        Integer value = jdbcTemplate.queryForObject("select value from `platform_uuid` where table_name = ? for update", Integer.class, tableName);
        value = value + 1;
        jdbcTemplate.update("update `platform_uuid` set value = ? where table_name = ?", value, tableName);
        return value.longValue();
    }

//    public static Long randomUUIDLong() {
//        JdbcTemplate jdbcTemplate = Platform.getBean(JdbcTemplate.class);
//        return jdbcTemplate.queryForObject("select uuid_short() from dual", Long.class);
//    }

    public static String randomUUIDString() {
        JdbcTemplate jdbcTemplate = Platform.getBean(JdbcTemplate.class);
        return jdbcTemplate.queryForObject("select uuid() from dual", String.class);
    }

    public static long saveFile(File file) {
        NamedParameterJdbcTemplate named = Platform.getBean(NamedParameterJdbcTemplate.class);

        XMLPropertiesConfiguration configuration = Platform.getBean(XMLPropertiesConfiguration.class);

        String patternFolder = configuration.getString(Configuration.PATTERN_FOLDER);

        String repo = configuration.getString(Configuration.RESOURCE_REPO);

        String fileRepo = DateFormatUtils.format(new Date(), patternFolder);
        File container = new File(repo, fileRepo);
        String extension = StringUtils.lowerCase(FilenameUtils.getExtension(file.getName()));


        String uuid = Platform.randomUUIDString();
        String name = uuid + "." + extension;
        container.mkdirs();
        try {
            FileUtils.copyFile(file, new File(container, name));
        } catch (Exception e) {
        }

        long length = file.length();
        String path = fileRepo;
        String mime = parseMimeType(file.getName());
        String label = file.getName();

        Long fileId = Platform.randomUUIDLong("platform_file");
        InsertQuery insertQuery = new InsertQuery("platform_file");
        insertQuery.addValue("platform_file_id = :file_id", fileId);
        insertQuery.addValue("path = :path", path);
        insertQuery.addValue("mime = :mime", mime);
        insertQuery.addValue("extension = :extension", extension);
        insertQuery.addValue("`length` = :length", length);
        insertQuery.addValue("label = :label", label);
        insertQuery.addValue("name = :name", name);
        named.update(insertQuery.toSQL(), insertQuery.getParam());
        return fileId;
    }

    public static String parseMimeType(String filename) {
        String extension = FilenameUtils.getExtension(filename);
        if (StringUtils.equalsIgnoreCase("png", extension)) {
            return "image/png";
        } else if (StringUtils.equalsIgnoreCase("jpg", extension)) {
            return "image/jpg";
        } else if (StringUtils.equalsIgnoreCase("gif", extension)) {
            return "image/gif";
        } else if (StringUtils.equalsIgnoreCase("tiff", extension)) {
            return "image/tiff";
        } else if (StringUtils.equalsIgnoreCase("txt", extension)) {
            return "text/plain";
        } else {
            return "application/octet-stream";
        }
    }

    public static <T> T getBean(String name, Class<T> requiredType) {
        if (servletContext == null) {
            servletContext = WebApplication.get().getServletContext();
        }
        ApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        return applicationContext.getBean(name, requiredType);
    }


    public static <T> T getBean(Class<T> requiredType) {
        if (servletContext == null) {
            servletContext = WebApplication.get().getServletContext();
        }
        ApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        return applicationContext.getBean(requiredType);
    }
}
