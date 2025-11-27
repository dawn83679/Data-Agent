package edu.zsc.ai.plugin.model.command.sql;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 为什么不能直接 connection.getAutoCommit() 获取？
 * 
 * 这个测试类演示了为什么必须先定义变量，再在 try 块中赋值。
 * 
 * 核心原因：
 * 1. connection.getAutoCommit() 抛出受检异常 SQLException
 * 2. finally 块需要访问这个变量来恢复状态
 * 3. Java 的变量作用域规则限制了变量的可见性
 * 
 * @author Data-Agent Team
 */
public class AutoCommitDeclarationTest {

    /**
     * ==================== 问题演示 ====================
     */

    /**
     * ❌ 错误方式 1：直接在声明时调用
     * 
     * 为什么不能这样写？
     * - connection.getAutoCommit() 会抛出 SQLException（受检异常）
     * - 变量声明语句不在 try-catch 块内，无法捕获异常
     * - 编译器会报错：Unhandled exception type SQLException
     */
    public void wrongWay1_DirectCall(Connection connection) {
        System.out.println("\n❌ 错误方式 1：直接在声明时调用");
        System.out.println("代码：boolean originalAutoCommit = connection.getAutoCommit();");
        System.out.println("问题：编译错误！SQLException 是受检异常，必须被捕获或声明");
        System.out.println("错误信息：Unhandled exception type SQLException");
        
        // 取消注释下面这行代码会导致编译错误：
        // boolean originalAutoCommit = connection.getAutoCommit();  // ❌ 编译失败！
    }

    /**
     * ❌ 错误方式 2：在 try 块内声明
     * 
     * 为什么不能这样写？
     * - 虽然可以捕获异常，但变量作用域仅限于 try 块
     * - finally 块无法访问在 try 块内声明的变量
     * - 编译器会报错：Cannot resolve symbol 'originalAutoCommit'
     */
    public void wrongWay2_DeclareInsideTry(Connection connection) {
        System.out.println("\n❌ 错误方式 2：在 try 块内声明");
        System.out.println("代码：");
        System.out.println("  try {");
        System.out.println("      boolean originalAutoCommit = connection.getAutoCommit();");
        System.out.println("  } finally {");
        System.out.println("      connection.setAutoCommit(originalAutoCommit);  // ❌ 无法访问！");
        System.out.println("  }");
        System.out.println("问题：变量作用域仅限于 try 块，finally 块无法访问");
        
        try {
            boolean originalAutoCommit = connection.getAutoCommit();
            System.out.println("在 try 块内可以访问：" + originalAutoCommit);
            
        } catch (SQLException e) {
            System.err.println("捕获异常：" + e.getMessage());
            
        } finally {
            // 取消注释下面这行代码会导致编译错误：
            // connection.setAutoCommit(originalAutoCommit);  // ❌ 编译失败！
            System.out.println("在 finally 块内无法访问 originalAutoCommit 变量");
        }
    }

    /**
     * ❌ 错误方式 3：添加 throws 声明也无济于事
     * 
     * 为什么不能这样写？
     * - 即使方法声明 throws SQLException，变量作用域问题依然存在
     * - 在 try 块内声明的变量，finally 块仍然无法访问
     */
    public void wrongWay3_WithThrows(Connection connection) throws SQLException {
        System.out.println("\n❌ 错误方式 3：添加 throws 声明");
        System.out.println("即使方法声明了 throws SQLException，作用域问题依然存在");
        
        try {
            boolean originalAutoCommit = connection.getAutoCommit();
            System.out.println("在 try 块内：" + originalAutoCommit);
            
        } finally {
            // 仍然无法访问 originalAutoCommit
            System.out.println("在 finally 块内：仍然无法访问变量");
        }
    }

    /**
     * ==================== 正确方式 ====================
     */

    /**
     * ✅ 正确方式：先声明，再赋值
     * 
     * 为什么这样可以？
     * 1. 变量在 try-catch-finally 外部声明，所有块都可以访问
     * 2. 在 try 块内赋值，可以捕获 SQLException
     * 3. 在 finally 块内可以访问变量来恢复状态
     * 4. 给定默认值 true，即使获取失败也有合理的回退值
     */
    public void correctWay_DeclareOutsideAssignInside(Connection connection) {
        System.out.println("\n✅ 正确方式：先声明，再赋值");
        System.out.println("代码：");
        System.out.println("  boolean originalAutoCommit = true;  // 在外部声明，给定默认值");
        System.out.println("  try {");
        System.out.println("      originalAutoCommit = connection.getAutoCommit();  // 在 try 内赋值");
        System.out.println("  } finally {");
        System.out.println("      connection.setAutoCommit(originalAutoCommit);  // ✅ 可以访问！");
        System.out.println("  }");
        
        // 1. 在外部声明变量，给定默认值
        boolean originalAutoCommit = true;
        
        try {
            // 2. 在 try 块内赋值，可以捕获异常
            originalAutoCommit = connection.getAutoCommit();
            System.out.println("✅ 成功获取 autoCommit 状态：" + originalAutoCommit);
            
            // 3. 修改状态
            connection.setAutoCommit(false);
            System.out.println("✅ 已禁用 autoCommit");
            
        } catch (SQLException e) {
            System.err.println("⚠️ 发生异常：" + e.getMessage());
            
        } finally {
            // 4. 在 finally 块内可以访问变量来恢复状态
            try {
                connection.setAutoCommit(originalAutoCommit);
                System.out.println("✅ 成功恢复 autoCommit 状态为：" + originalAutoCommit);
            } catch (SQLException e) {
                System.err.println("⚠️ 恢复 autoCommit 失败：" + e.getMessage());
            }
        }
    }

    /**
     * ==================== 作用域演示 ====================
     */

    /**
     * 演示 Java 变量作用域规则
     */
    public void demonstrateVariableScope() {
        System.out.println("\n📚 Java 变量作用域规则演示");
        System.out.println("=".repeat(50));
        
        // 场景 1：外部声明的变量
        System.out.println("\n场景 1：外部声明的变量");
        int outerVariable = 100;
        System.out.println("声明位置：try-finally 外部");
        System.out.println("初始值：" + outerVariable);
        
        try {
            outerVariable = 200;
            System.out.println("  try 块内：可以访问和修改 = " + outerVariable);
        } finally {
            System.out.println("  finally 块内：可以访问 = " + outerVariable);
        }
        System.out.println("  外部：可以访问 = " + outerVariable);
        System.out.println("结论：✅ 外部声明的变量，所有地方都可以访问");
        
        // 场景 2：内部声明的变量
        System.out.println("\n场景 2：内部声明的变量");
        System.out.println("声明位置：try 块内部");
        
        try {
            int innerVariable = 300;
            System.out.println("  try 块内：可以访问 = " + innerVariable);
        } finally {
            // innerVariable 在这里不可见
            System.out.println("  finally 块内：❌ 无法访问 innerVariable");
        }
        // innerVariable 在这里也不可见
        System.out.println("  外部：❌ 无法访问 innerVariable");
        System.out.println("结论：❌ 内部声明的变量，作用域仅限于声明的块");
    }

    /**
     * ==================== 实际应用场景 ====================
     */

    /**
     * 模拟 AbstractSqlExecutor 的实际使用场景
     */
    public void realWorldScenario(Connection connection, String sql) {
        System.out.println("\n🎯 实际应用场景：事务管理");
        System.out.println("=".repeat(50));
        
        // 必须在外部声明，这样 finally 块才能访问
        boolean originalAutoCommit = true;
        boolean transactionStarted = false;
        
        try {
            // 步骤 1：保存原始状态
            originalAutoCommit = connection.getAutoCommit();
            System.out.println("1️⃣ 原始 autoCommit 状态：" + originalAutoCommit);
            
            // 步骤 2：开启事务
            connection.setAutoCommit(false);
            transactionStarted = true;
            System.out.println("2️⃣ 已开启事务（autoCommit = false）");
            
            // 步骤 3：执行 SQL
            connection.createStatement().execute(sql);
            System.out.println("3️⃣ SQL 执行成功");
            
            // 步骤 4：提交事务
            connection.commit();
            System.out.println("4️⃣ 事务已提交");
            
        } catch (SQLException e) {
            System.err.println("❌ 执行失败：" + e.getMessage());
            
            // 步骤 5：回滚事务
            if (transactionStarted) {
                try {
                    connection.rollback();
                    System.out.println("5️⃣ 事务已回滚");
                } catch (SQLException rollbackEx) {
                    System.err.println("❌ 回滚失败：" + rollbackEx.getMessage());
                }
            }
            
        } finally {
            // 步骤 6：恢复原始状态
            // 这就是为什么必须在外部声明 originalAutoCommit！
            if (transactionStarted) {
                try {
                    connection.setAutoCommit(originalAutoCommit);
                    System.out.println("6️⃣ 已恢复 autoCommit 状态为：" + originalAutoCommit);
                } catch (SQLException e) {
                    System.err.println("⚠️ 恢复状态失败：" + e.getMessage());
                }
            }
        }
    }

    /**
     * ==================== 总结 ====================
     */

    /**
     * 主方法：运行所有演示
     */
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║  为什么不能直接 connection.getAutoCommit() 获取？        ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        
        AutoCommitDeclarationTest test = new AutoCommitDeclarationTest();
        
        // 演示错误方式
        test.wrongWay1_DirectCall(null);
        test.wrongWay2_DeclareInsideTry(null);
        
        // 演示作用域规则
        test.demonstrateVariableScope();
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📝 核心要点总结");
        System.out.println("=".repeat(60));
        System.out.println();
        System.out.println("❌ 不能直接调用的原因：");
        System.out.println("   1. connection.getAutoCommit() 抛出受检异常 SQLException");
        System.out.println("   2. 变量声明语句不在 try-catch 块内，无法捕获异常");
        System.out.println("   3. 编译器会报错：Unhandled exception type SQLException");
        System.out.println();
        System.out.println("❌ 不能在 try 块内声明的原因：");
        System.out.println("   1. Java 变量作用域规则：变量只在声明的块内可见");
        System.out.println("   2. finally 块需要访问这个变量来恢复状态");
        System.out.println("   3. 如果在 try 内声明，finally 块无法访问");
        System.out.println();
        System.out.println("✅ 正确的做法：");
        System.out.println("   1. 在 try-catch-finally 外部声明变量");
        System.out.println("   2. 给定一个合理的默认值（如 true）");
        System.out.println("   3. 在 try 块内赋值，可以捕获异常");
        System.out.println("   4. 在 finally 块内访问变量，恢复状态");
        System.out.println();
        System.out.println("💡 AbstractSqlExecutor 的实现：");
        System.out.println("   boolean originalAutoCommit = true;  // 外部声明");
        System.out.println("   try {");
        System.out.println("       originalAutoCommit = getOriginalAutoCommit(connection);");
        System.out.println("       // ... 执行 SQL ...");
        System.out.println("   } finally {");
        System.out.println("       restoreAutoCommit(connection, command, originalAutoCommit);");
        System.out.println("   }");
        System.out.println();
        System.out.println("=".repeat(60));
    }
}
