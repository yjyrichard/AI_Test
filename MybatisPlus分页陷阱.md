<br />

## MybatisPlus分页陷阱：为何我的多表连查总是“吞掉”数据？

在Java开发中，MybatisPlus（简称MP）以其强大的功能和极简的API深受喜爱，尤其是它的分页插件，几乎是项目标配。然而，当我们将它与稍显复杂的多表`JOIN`查询结合使用时，一个诡异的问题常常困扰着开发者：**“我明明设置了每页10条，为什么结果只返回了3条、5条，甚至更少？”**

这篇文章将带你彻底搞懂这个问题的根源，并提供清晰、可行的解决方案。

### 一、风平浪静：单表分页是如何工作的？

在揭开谜底之前，我们先回顾一下最简单、最理想的情况：单表分页。

假设我们有一个用户表 `user`。

```sql
CREATE TABLE user (
  id BIGINT PRIMARY KEY,
  name VARCHAR(50),
  age INT
);
-- 假设里面有100条数据
```

在Java中，我们使用MP的分页插件进行查询：

```java
// Service层代码
@Autowired
private UserMapper userMapper;

public IPage<User> findUsersByPage(int pageNum, int pageSize) {
    // 1. 创建分页对象 Page
    Page<User> page = new Page<>(pageNum, pageSize);
    
    // 2. 调用MP内置的selectPage方法
    // UserMapper继承自BaseMapper<User>
    IPage<User> userPage = userMapper.selectPage(page, null); // 第二个参数是查询条件，这里为null表示查所有
    
    return userPage;
}
```

这个过程背后，MybatisPlus分页插件为你做了两件非常重要的事：

1. **执行COUNT查询**：它会自动生成一条SQL来查询总记录数。
   ```sql
   SELECT COUNT(*) FROM user
   ```
   这个结果（比如100）会被设置到返回的`IPage`对象的`total`属性中。

2. **执行分页数据查询**：它会改写你原本的查询SQL，在末尾加上`LIMIT`语句。
   ```sql
   -- 假设查询第一页，每页10条 (pageNum=1, pageSize=10)
   SELECT id, name, age FROM user LIMIT 0, 10
   ```
   查询出的这10条记录，会被设置到`IPage`对象的`records`属性中。

一切看起来都那么美好，`total`是100，`records`不多不少正好10条。

### 二、问题浮现：当“一对多”遇上分页

现在，让我们引入复杂度。假设我们还有一个帖子表 `post`，一个用户可以发布多篇帖子，这是一个典型的“一对多”关系。

**用户表 (user)**

| id | name |
| :- | :--- |
| 1  | 张三   |
| 2  | 李四   |
| 3  | 王五   |

**帖子表 (post)**

| id  | title | user\_id |
| :-- | :---- | :------- |
| 101 | 帖子A   | 1        |
| 102 | 帖子B   | 1        |
| 103 | 帖子C   | 1        |
| 104 | 帖子D   | 2        |
| 105 | 帖子E   | 2        |

我们的需求是：**分页查询用户信息，并同时带出他们发布的所有帖子**。

很自然，我们会想到用`LEFT JOIN`。于是，我们自定义一个Mapper方法。

```java
// 定义一个VO来接收结果
@Data
public class UserPostVO {
    private Long userId;
    private String userName;
    private List<Post> posts; // 一个用户对应多篇帖子
}

// Mapper接口
public interface UserMapper extends BaseMapper<User> {
    // 自定义多表连接分页查询
    IPage<UserPostVO> selectUserWithPosts(IPage<?> page);
}

//对应的XML文件 UserMapper.xml
<select id="selectUserWithPosts" resultMap="userWithPostsResultMap">
    SELECT
        u.id as user_id,
        u.name as user_name,
        p.id as post_id,
        p.title as post_title
    FROM
        user u
    LEFT JOIN
        post p ON u.id = p.user_id
</select>

<!-- 定义结果集映射 -->
<resultMap id="userWithPostsResultMap" type="com.example.vo.UserPostVO">
    <id property="userId" column="user_id"/>
    <result property="userName" column="user_name"/>
    <collection property="posts" ofType="com.example.entity.Post">
        <id property="id" column="post_id"/>
        <result property="title" column="post_title"/>
    </collection>
</resultMap>
```

现在，我们在Service层调用这个方法，期望每页查询2个用户：

```java
// Service层调用
public IPage<UserPostVO> findUserPostsByPage() {
    // 期望：查询第一页，每页2个用户
    Page<UserPostVO> page = new Page<>(1, 2); 
    return userMapper.selectUserWithPosts(page);
}
```

**你期望的结果是**：返回包含“张三”和“李四”的两条记录。

**但实际的结果是**：只返回了“张三”这一条记录！`page.getRecords().size()` 的结果是 `1`，而不是 `2`。

为什么会这样？**问题就出在数据库执行的真实SQL上。**

### 三、深度剖析：数据库视角与程序视角的“鸿沟”

让我们扮演一次MybatisPlus分页插件，看看它会如何处理我们写的SQL。

我们写的原始SQL是：

```sql
SELECT u.id, u.name, p.id, p.title FROM user u LEFT JOIN post p ON u.id = p.user_id
```

在不加任何限制的情况下，数据库执行这条`JOIN`语句，得到的 **物理结果集** 是这样的：

| user\_id | user\_name | post\_id | post\_title |
| :------- | :--------- | :------- | :---------- |
| 1        | 张三         | 101      | 帖子A         |
| 1        | 张三         | 102      | 帖子B         |
| 1        | 张三         | 103      | 帖子C         |
| 2        | 李四         | 104      | 帖子D         |
| 2        | 李四         | 105      | 帖子E         |
| 3        | 王五         | NULL     | NULL        |

看到了吗？因为“一对多”的关系，**用户“张三”在物理结果集中出现了3次，“李四”出现了2次**。

现在，分页插件开始工作了。它拿到我们的SQL，并简单粗暴地在末尾加上 `LIMIT`。我们设置的`pageSize`是2。

**分页插件生成的最终SQL:**

```sql
SELECT u.id, u.name, p.id, p.title 
FROM user u 
LEFT JOIN post p ON u.id = p.user_id 
LIMIT 0, 2 -- 取前2条记录
```

数据库忠实地执行了这条指令，它从上面的物理结果集中，取出了 **前2行**：

| user\_id | user\_name | post\_id | post\_title |
| :------- | :--------- | :------- | :---------- |
| 1        | 张三         | 101      | 帖子A         |
| 1        | 张三         | 102      | 帖子B         |

这两行数据返回给Mybatis后，Mybatis再根据你的`<resultMap>`进行组装。它发现这两行数据的`user_id`都是1，于是它创建了一个`UserPostVO`对象，把ID为1的张三的信息放进去，然后把这两行对应的帖子（帖子A和帖子B）都放进`posts`这个List里。

最终，组装完成后，你的程序只得到了 **一个** `UserPostVO` 对象（代表张三）。

**这就是问题的根源：**

> **分页插件的** **`LIMIT`** **是作用在** **`JOIN`** **之后的物理行上的，而不是作用在你逻辑上期望的“主表实体”上的。**

你想要2个**用户**，但`LIMIT 2`给了你2条**物理行记录**。因为“一对多”的数据冗余，这2条物理行记录可能恰好都属于同1个用户。于是，最终在Java层面聚合后，你就只得到了1个用户对象，数量自然就比预期的少了。

### 四、拨乱反正：如何正确地进行多表分页？

知道了原因，解决方案也就清晰了。核心思想是：**分页必须针对主表，关联查询只是为了填充数据。**

#### 方案一（推荐）：先分页查主表ID，再关联查完整数据

这是最通用、最高效且逻辑最清晰的解决方案。

**第一步：分页查询出主表（`user`）的ID列表。**

这一步非常纯粹，就是单表分页，绝对不会出错。

```java
// Mapper接口中增加一个方法
IPage<User> selectPage(IPage<User> page, @Param("ew") Wrapper<User> queryWrapper);

// Service层
public IPage<UserPostVO> findUserPostsByPageCorrectly(int pageNum, int pageSize) {
    // 1. 创建Page对象，注意泛型是主表实体User
    Page<User> page = new Page<>(pageNum, pageSize);

    // 2. 只对主表进行分页查询
    IPage<User> userPage = userMapper.selectPage(page, null);

    // 3. 如果分页结果为空，直接返回一个空的IPage
    List<User> userRecords = userPage.getRecords();
    if (userRecords.isEmpty()) {
        return new Page<>(pageNum, pageSize);
    }
    
    // 4. 提取主表的ID列表
    List<Long> userIds = userRecords.stream().map(User::getId).collect(Collectors.toList());

    // 5. 根据ID列表，一次性查询出所有关联的数据（这里可以用之前写的JOIN方法，但要改动）
    List<UserPostVO> voList = userMapper.selectUserWithPostsByIds(userIds);

    // 6. 手动组装最终的IPage对象并返回
    IPage<UserPostVO> resultPage = new Page<>(pageNum, pageSize, userPage.getTotal());
    resultPage.setRecords(voList);
    
    return resultPage;
}

// UserMapper.xml 中需要一个根据ID列表查询的方法
<select id="selectUserWithPostsByIds" resultMap="userWithPostsResultMap">
    SELECT
        u.id as user_id,
        u.name as user_name,
        p.id as post_id,
        p.title as post_title
    FROM
        user u
    LEFT JOIN
        post p ON u.id = p.user_id
    WHERE
        u.id IN
        <foreach item="id" collection="list" open="(" separator="," close=")">
            #{id}
        </foreach>
</select>
```

**优点：**

* **分页逻辑正确**：分页只作用于主表，`LIMIT`的数量和最终得到的实体数量完全一致。
* **性能更优**：`JOIN`操作只在分页后的一小部分数据上进行，而不是全表`JOIN`后再分页，大大减少了数据库的计算量。
* **逻辑清晰**：分页和数据填充分离，代码易于理解和维护。

#### 方案二：在XML中使用嵌套查询（N+1问题警告）

Mybatis的`<collection>`标签支持嵌套查询（`select`属性），它也能解决数据不一致的问题，但要小心潜在的性能陷阱。

```xml
<!-- UserMapper.xml -->
<select id="selectUsersByPage" resultType="com.example.entity.User">
    SELECT id, name FROM user LIMIT #{page.offset}, #{page.size}
</select>

<select id="findPostsByUserId" resultType="com.example.entity.Post">
    SELECT id, title FROM post WHERE user_id = #{userId}
</select>

<resultMap id="userWithPostsResultMapLazy" type="com.example.vo.UserPostVO">
    <id property="userId" column="id"/>
    <result property="userName" column="name"/>
    <!-- 
        column="id" 把主查询查出的id字段，传给select指定的查询语句
    -->
    <collection property="posts" ofType="com.example.entity.Post"
                column="id" select="findPostsByUserId"/>
</resultMap>
```

这种方式，会先执行`selectUsersByPage`查出N个用户，然后对这N个用户，**每一个**都去执行一次`findPostsByUserId`查询。总共执行了 `1 + N` 次查询，这就是臭名昭著的“N+1问题”。在数据量大时，会对数据库造成巨大压力。

虽然可以通过开启延迟加载（`lazyLoadingEnabled=true`）来缓解，但本质问题依然存在。因此，**方案一通常是更优的选择**。

### 总结

最后，让我们用一句话来概括这个问题的核心：

**MybatisPlus分页插件是对SQL物理行的分页，而“一对多”的`JOIN`查询会导致主表数据在物理行上重复，从而引发分页计数的“错觉”。**

当你再遇到多表分页查出数据变少的问题时，请立刻想起这个根本原因，并采用\*\*“先分页查主表ID，再关联查询完整数据”\*\*的黄金法则来解决它。这样，你的分页查询将永远稳如磐石。

<br />

<br />

当然！你提出的这个问题非常棒，直击了MybatisPlus（MP）分页插件设计的核心！很多人只是“会用”，但并不明白其背后的原理。下面，我就为你生成一篇文章，详细且通俗地解释这个“方法规则”背后的原因。

***

## 揭秘MybatisPlus分页插件：为何方法签名必须遵守“潜规则”？

在使用MybatisPlus的分页插件时，有经验的开发者都会遵循一个不成文的规定：自定义Mapper分页查询时，方法签名（Method Signature）必须是特定的格式。

就像你提供的例子一样：

```java
// 返回值是 IPage
IPage<Question> getCategoryQuestionCount(
    // 第一个参数是 IPage
    IPage page, 
    // 其他自定义参数
    @Param("queryVo") QuestionQueryVo questionQueryVo
);
```

这个规则可以总结为：

1. **返回值** 必须是 `IPage<T>` 或其子类（如 `Page<T>`）。
2. **第一个参数** 必须是 `IPage<T>` 或其子类。

很多初学者会感到困惑：“这是Java的语法规定吗？为什么不能返回 `List<T>`？为什么必须把 `IPage` 作为第一个参数？”

答案是：**这并非Java语言的强制规定，而是MybatisPlus分页插件为了实现其“自动化”分页功能，与开发者之间定下的一个“契约”或“约定”。**

为了理解这个“契约”，我们需要先搞懂MP分页插件的“工作模式”。

### 一、把插件想象成一个“聪明的管家”

你可以把MP分页插件想象成一个非常智能的管家，这个管家（在程序里叫 **拦截器 Interceptor**）时刻待命。当你要去数据库“取东西”（执行SQL查询）时，他会主动介入，帮你处理一些繁琐的杂事。

分页查询就是一件很繁琐的事，它至少包含两个步骤：

1. 查询符合条件的总共有多少条数据（`SELECT COUNT(*)...`）。
2. 查询当前页需要展示的数据（`SELECT * ... LIMIT X, Y`）。

如果没有管家（插件），你就得自己写两条SQL，调用两次Mapper方法。但有了这个聪明的管家，你只需要下达一个“分页取货”的指令，他就能自动帮你完成这两件事，并把结果整理好交给你。

那么，管家如何知道你这次的指令是“分页取货”，而不是普通的“取货”呢？

**这就是“契约”起作用的地方！**

### 二、解读“契约”：方法签名背后的秘密

这个“契约”就是你的方法签名。管家（拦截器）会“偷听”你对数据库发出的每一个指令（每一次Mapper方法调用）。当它发现某个指令完全符合你们之间的约定格式时，它就知道：“哦！主人这次是想分页，轮到我大显身手了！”

#### 1. 为什么第一个参数必须是 `IPage`？（管家的“信号”和“输入”）

`IPage` 参数对于管家来说，有两个至关重要的作用：

* **识别信号（Signal）**：这是最重要的作用。管家通过检查你调用的方法，**看到第一个参数是** **`IPage`** **类型**，就立刻识别出这是一个分页请求。这就像你给了他一个特殊的“手势”，他一看就懂。如果你的方法没有这个参数，管家就会认为你只是想执行一个普通查询，于是他就“袖手旁观”，不会进行任何分页处理。

* **传递指令（Input）**：你希望查询第几页？每页显示多少条？这些信息总得告诉管家吧。`IPage` 对象就是你传递这些指令的载体。
  ```java
  // 在调用前，你设置好了分页参数
  Page<Question> page = new Page<>(1, 10); // 我要第 1 页，每页 10 条
  mapper.getCategoryQuestionCount(page, vo); // 把指令交给管家
  ```
  管家拿到这个 `page` 对象后，会从中读取 `current=1` 和 `size=10`，这样它才知道后续该如何拼接 `LIMIT` 语句（`LIMIT 0, 10`）。

#### 2. 为什么返回值必须是 `IPage`？（管家的“结果容器”）

当你告诉管家要分页查询后，他辛辛苦苦地帮你执行了两个SQL：一个`COUNT`，一个`LIMIT`。现在他拿到了两份结果：

1. 总记录数（比如 `123`）。
2. 当前页的数据列表（一个包含10个 `Question` 对象的 `List`）。

如果你的方法返回值是 `List<Question>`，那管家怎么把“总记录数 `123`”这个重要的信息交给你呢？没地方放啊！

所以，**`IPage`** **返回值就是一个精心设计的“结果容器”**。

管家会把他获取到的所有分页相关信息，全部打包塞进这个 `IPage` 对象里再返回给你。

* `page.setRecords(查询到的数据列表)`
* `page.setTotal(查询到的总记录数)`
* 他还会帮你计算好总页数 `page.setPages(...)`

当你拿到这个返回的 `IPage` 对象时，所有你需要的分页信息都一应俱全，可以直接返回给前端用于展示分页组件。

### 三、一次完整的“幕后工作流程”

让我们以你的代码为例，看看一次完整的调用背后，到底发生了什么：

**你的代码 (Service层):**

```java
// 1. 创建指令对象，并填好分页要求
Page<Question> page = new Page<>(1, 10);
QuestionQueryVo vo = new QuestionQueryVo();
vo.setCategoryId(1L);

// 2. 调用Mapper方法，下达指令
IPage<Question> resultPage = questionMapper.getCategoryQuestionCount(page, vo);

// 6. 从结果容器中获取数据
long total = resultPage.getTotal(); // 获取总数
List<Question> records = resultPage.getRecords(); // 获取当前页数据
```

**Mapper XML:**

```xml
<select id="getCategoryQuestionCount" resultType="com.example.Question">
    SELECT * FROM question
    <where>
        <if test="queryVo.categoryId != null">
            category_id = #{queryVo.categoryId}
        </if>
    </where>
    <!-- 注意：你自己写的SQL里，完全不需要写LIMIT！ -->
</select>
```

**管家（MP分页插件）的幕后工作：**

1. **拦截**：你调用 `getCategoryQuestionCount` 的动作被MP的 `PaginationInterceptor` 拦截了。

2. **识别**：拦截器检查该方法的签名，发现第一个参数是 `IPage` 类型。**“分页任务，启动！”**

3. **分析**：它拿到你传入的 `page` 对象，得知 `current=1`, `size=10`。

4. **改写与执行（第一步）**：它把你XML里的SQL拿过来，进行智能改写，生成一条`COUNT`语句，然后执行它。
   ```sql
   -- 自动生成的COUNT SQL
   SELECT COUNT(*) FROM question WHERE category_id = 1;
   ```
   假设查询结果是 `123`。

5. **填充结果（一部分）**：拦截器将 `123`这个总数，设置回你传入的那个 `page` 对象里：`page.setTotal(123)`。

6. **改写与执行（第二步）**：拦截器再次把你XML里的SQL拿过来，根据分页参数，在末尾追加上物理分页语句（比如MySQL的`LIMIT`）。
   ```sql
   -- 自动追加LIMIT的查询SQL
   SELECT * FROM question WHERE category_id = 1 LIMIT 0, 10;
   ```
   它执行这条SQL，得到了一个包含10个 `Question` 对象的 `List`。

7. **填充结果（另一部分）**：拦截器将这个 `List` 设置到同一个 `page` 对象里：`page.setRecords(theListOf10Questions)`。

8. **返回**：此时，最初那个 `page` 对象已经被填满了所有分页数据。拦截器将这个 **被修改并填充好** 的 `page` 对象作为最终的返回值，交还给你的Service层代码。

### 总结

现在，我们再回头看那个“方法规则”，就豁然开朗了：

* **`IPage`** **作为第一个参数**：是给MP分页插件的**暗号和指令**，告诉它“我要分页”，并把分页要求（第几页、每页几条）传递给它。
* **`IPage`** **作为返回值**：是MP分页插件为你准备的**万能结果集**，里面不仅有你想要的数据列表（`records`），更有分页必不可少的总记录数（`total`）等信息。

这个设计是一个非常经典的“约定优于配置”（Convention over Configuration）思想的体现。你无需进行任何复杂的配置，只需遵循这个简单的方法签名约定，就能享受到MybatisPlus为你提供的强大而便捷的自动化分页功能。
