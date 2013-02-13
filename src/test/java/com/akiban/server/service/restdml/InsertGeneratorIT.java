/**
 * END USER LICENSE AGREEMENT (“EULA”)
 *
 * READ THIS AGREEMENT CAREFULLY (date: 9/13/2011):
 * http://www.akiban.com/licensing/20110913
 *
 * BY INSTALLING OR USING ALL OR ANY PORTION OF THE SOFTWARE, YOU ARE ACCEPTING
 * ALL OF THE TERMS AND CONDITIONS OF THIS AGREEMENT. YOU AGREE THAT THIS
 * AGREEMENT IS ENFORCEABLE LIKE ANY WRITTEN AGREEMENT SIGNED BY YOU.
 *
 * IF YOU HAVE PAID A LICENSE FEE FOR USE OF THE SOFTWARE AND DO NOT AGREE TO
 * THESE TERMS, YOU MAY RETURN THE SOFTWARE FOR A FULL REFUND PROVIDED YOU (A) DO
 * NOT USE THE SOFTWARE AND (B) RETURN THE SOFTWARE WITHIN THIRTY (30) DAYS OF
 * YOUR INITIAL PURCHASE.
 *
 * IF YOU WISH TO USE THE SOFTWARE AS AN EMPLOYEE, CONTRACTOR, OR AGENT OF A
 * CORPORATION, PARTNERSHIP OR SIMILAR ENTITY, THEN YOU MUST BE AUTHORIZED TO SIGN
 * FOR AND BIND THE ENTITY IN ORDER TO ACCEPT THE TERMS OF THIS AGREEMENT. THE
 * LICENSES GRANTED UNDER THIS AGREEMENT ARE EXPRESSLY CONDITIONED UPON ACCEPTANCE
 * BY SUCH AUTHORIZED PERSONNEL.
 *
 * IF YOU HAVE ENTERED INTO A SEPARATE WRITTEN LICENSE AGREEMENT WITH AKIBAN FOR
 * USE OF THE SOFTWARE, THE TERMS AND CONDITIONS OF SUCH OTHER AGREEMENT SHALL
 * PREVAIL OVER ANY CONFLICTING TERMS OR CONDITIONS IN THIS AGREEMENT.
 */
package com.akiban.server.service.restdml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.akiban.ais.model.TableName;
import com.akiban.qp.operator.Operator;
import com.akiban.server.explain.ExplainContext;
import com.akiban.server.explain.format.DefaultFormatter;
import com.akiban.server.service.session.Session;
import com.akiban.server.t3expressions.T3RegistryService;
import com.akiban.server.test.it.ITBase;

public class InsertGeneratorIT extends ITBase {

    public static final String SCHEMA = "test";
    private InsertGenerator insertGenerator;
    
    @After
    public void commit() {
        this.txnService().commitTransaction(this.session());
    }

    @Before
    public void start() {
        Session session = this.session();
        this.txnService().beginTransaction(session);
    }
    
    @Test
    public void testCInsert() {
        
        createTable(SCHEMA, "c",
                "cid INT PRIMARY KEY NOT NULL",
                "name VARCHAR(32)");

        TableName table = new TableName (SCHEMA, "c");
        this.insertGenerator = new InsertGenerator (this.ais());
        insertGenerator.setT3Registry(this.serviceManager().getServiceByClass(T3RegistryService.class));
        Operator insert = insertGenerator.create(table);
        
        assertEquals(
                getExplain(insert, table.getSchemaName()),
                "\n  Project_Default(Field(0))\n" +
                "    Insert_Returning(INTO c)\n" +
                "      Project_Default(Field(0), Field(1))\n" +
                "        ValuesScan_Default([$1, $2])");
    }

    @Test
    public void testNoPKInsert() {
        
        createTable (SCHEMA, "c", 
                "cid INT NOT NULL",
                "name VARCHAR(32)");
        TableName table = new TableName (SCHEMA, "c");
        this.insertGenerator = new InsertGenerator (this.ais());
        insertGenerator.setT3Registry(this.serviceManager().getServiceByClass(T3RegistryService.class));
        Operator insert = insertGenerator.create(table);
        
        assertEquals(
                getExplain(insert, table.getSchemaName()),
                "\n  Insert_Returning(INTO c)\n" +
                "    Project_Default(Field(0), Field(1), NULL)\n" +
                "      ValuesScan_Default([$1, $2])");
    }

    @Test
    public void testIdentityDefault() {
        createTable (SCHEMA, "c",
                "cid int NOT NULL PRIMARY KEY generated by default as identity",
                "name varchar(32) NOT NULL");
        
        TableName table = new TableName (SCHEMA, "c");
        this.insertGenerator = new InsertGenerator (this.ais());
        insertGenerator.setT3Registry(this.serviceManager().getServiceByClass(T3RegistryService.class));
        Operator insert = insertGenerator.create(table);
        
        Pattern explain = Pattern.compile("\n  Project_Default\\(Field\\(0\\)\\)\n" +
                "    Insert_Returning\\(INTO c\\)\n" +
                "      Project_Default\\(ifnull\\(Field\\(0\\), NEXTVAL\\('test', '_sequence-3556597(\\$1)?'\\)\\), Field\\(1\\)\\)\n" +
                "        ValuesScan_Default\\(\\[\\$1, \\$2\\]\\)");
        assertTrue("Generated explain does not match test explain", explain.matcher(getExplain(insert, table.getSchemaName())).matches());
    }
    
    @Test
    public void testIdentityAlways() {
        createTable (SCHEMA, "c",
                "cid int NOT NULL PRIMARY KEY generated always as identity",
                "name varchar(32) NOT NULL");
        
        TableName table = new TableName (SCHEMA, "c");
        this.insertGenerator = new InsertGenerator (this.ais());
        insertGenerator.setT3Registry(this.serviceManager().getServiceByClass(T3RegistryService.class));
        Operator insert = insertGenerator.create(table);
        Pattern explain = Pattern.compile("\n  Project_Default\\(Field\\(0\\)\\)\n" +
                "    Insert_Returning\\(INTO c\\)\n" +
                "      Project_Default\\(NEXTVAL\\('test', '_sequence-3556597(\\$1)?'\\), Field\\(1\\)\\)\n" +
                "        ValuesScan_Default\\(\\[\\$1, \\$2\\]\\)");
        assertTrue("Generated explain does not match test explain", explain.matcher(getExplain(insert, table.getSchemaName())).matches());
    }
    
    @Test
    public void testDefaults() {
        createTable (SCHEMA, "c", 
                "cid int not null primary key default 0",
                "name varchar(32) not null default ''",
                "taxes double not null default '0.0'");
        
        TableName table = new TableName (SCHEMA, "c");
        this.insertGenerator = new InsertGenerator (this.ais());
        insertGenerator.setT3Registry(this.serviceManager().getServiceByClass(T3RegistryService.class));
        Operator insert = insertGenerator.create(table);
        assertEquals(
                getExplain(insert, table.getSchemaName()),
                "\n  Project_Default(Field(0))\n" +
                "    Insert_Returning(INTO c)\n" +
                "      Project_Default(ifnull(Field(0), 0), ifnull(Field(1), ''), ifnull(Field(2), 0.000000e+00))\n" +
                "        ValuesScan_Default([$1, $2, $3])");
    }
    
    @Test 
    public void testPKNotFirst() {
        createTable (SCHEMA, "c",
                "name varchar(32) not null",
                "address varchar(64) not null",
                "cid int not null primary key");
        TableName table = new TableName (SCHEMA, "c");
        this.insertGenerator = new InsertGenerator (this.ais());
        insertGenerator.setT3Registry(this.serviceManager().getServiceByClass(T3RegistryService.class));
        Operator insert = insertGenerator.create(table);
        assertEquals(
                getExplain(insert, table.getSchemaName()),
                "\n  Project_Default(Field(2))\n" +
                "    Insert_Returning(INTO c)\n" +
                "      Project_Default(Field(0), Field(1), Field(2))\n" +
                "        ValuesScan_Default([$1, $2, $3])");
    }
    
    @Test
    public void testPKMultiColumn() {
        createTable(SCHEMA, "o",
                "cid int not null",
                "oid int not null",
                "items int not null",
                "primary key (cid, oid)");
        TableName table = new TableName (SCHEMA, "o");
        this.insertGenerator = new InsertGenerator (this.ais());
        insertGenerator.setT3Registry(this.serviceManager().getServiceByClass(T3RegistryService.class));
        Operator insert = insertGenerator.create(table);
        assertEquals(
                getExplain(insert, table.getSchemaName()),
                "\n  Project_Default(Field(0), Field(1))\n" +
                "    Insert_Returning(INTO o)\n" +
                "      Project_Default(Field(0), Field(1), Field(2))\n" +
                "        ValuesScan_Default([$1, $2, $3])");
    }
    
    @Test
    public void testJoinedTable() {
        createTable(SCHEMA, "c",
                "cid int not null",
                "fist_name varchar(32)",
                "PRIMARY KEY(cid)");
        createTable (SCHEMA, "a",
                "aid int not null",
                "cid int not null",
                "state char(2)",
                "PRIMARY KEY (aid)",
                "GROUPING FOREIGN KEY (cid) REFERENCES c(cid)");
        TableName table = new TableName (SCHEMA, "a");
        this.insertGenerator = new InsertGenerator (this.ais());
        insertGenerator.setT3Registry(this.serviceManager().getServiceByClass(T3RegistryService.class));
        Operator insert = insertGenerator.create(table);
        assertEquals(
                getExplain(insert, table.getSchemaName()),
                "\n  Project_Default(Field(0))\n" +
                "    Insert_Returning(INTO a)\n" +
                "      Project_Default(Field(0), Field(1), Field(2))\n" +
                "        ValuesScan_Default([$1, $2, $3])");
    }

    protected String getExplain (Operator plannable, String defaultSchemaName) {
        StringBuilder str = new StringBuilder();
        ExplainContext context = new ExplainContext(); // Empty
        DefaultFormatter f = new DefaultFormatter(defaultSchemaName);
        for (String operator : f.format(plannable.getExplainer(context))) {
            str.append("\n  ");
            str.append(operator);
        }
        return str.toString();
    }
}