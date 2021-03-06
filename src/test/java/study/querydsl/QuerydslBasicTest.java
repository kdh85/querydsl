package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;


@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @PersistenceUnit
    EntityManagerFactory emf;

    @BeforeEach
    void setUp() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    void startJPQL() {
        Member findMember = em.createQuery("select m from Member m where" +
                " m.username=:username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQuerydsl() {

        //QMember m = new QMember("m");
        //QMember m = QMember.member;

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void search() {
        List<Member> findMembers = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetch();
        assertThat(findMembers).hasSize(1);
        assertThat(findMembers.get(0).getUsername()).isEqualTo("member1");
        assertThat(findMembers).filteredOn(m -> m.getUsername() == "member1");
        assertThat(findMembers).filteredOn(m -> "member1".equals(m.getUsername()));
    }

    @Test
    void resultFetch() {
        List<Member> fetch = queryFactory.selectFrom(member)
                .fetch();

        Member findMember = queryFactory.selectFrom(member)
                .fetchOne();

        Member fetchFirst = queryFactory.selectFrom(member)
                .fetchFirst();

        QueryResults<Member> memberQueryResults = queryFactory.selectFrom(member)
                .fetchResults();

        memberQueryResults.getTotal();
        List<Member> content = memberQueryResults.getResults();

        long count = queryFactory.selectFrom(member)
                .fetchCount();
    }

    @Test
    void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();

    }

    @Test
    void paging1() {
        List<Member> result = queryFactory.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)//?????? ???????????? ??????
                .limit(2)//????????? ????????? ??????
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    void paging2() {
        QueryResults<Member> memberQueryResults = queryFactory.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(memberQueryResults.getTotal()).isEqualTo(4);//?????? ??? ?????????.
        assertThat(memberQueryResults.getLimit()).isEqualTo(2);//limit ??????.
        assertThat(memberQueryResults.getOffset()).isEqualTo(1);//offset ??????.
        assertThat(memberQueryResults.getResults().size()).isEqualTo(2);//???????????? ?????????.
    }

    @Test
    void aggregation() {
        List<Tuple> result = queryFactory.select(
                member.count(),
                member.age.sum(),
                member.age.avg(),
                member.age.max(),
                member.age.min()
        )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);//???????????? ???????????? ?????????????????? queyrdsl?????? ???????????? ???????????? ????????? ???????????? ????????????.
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);

    }

    @Test
    void group() {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    @Test
    void join() {

        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /*
        ????????????.
        ????????? ????????? ???????????? ?????? ?????? ??????.(?????? ????????? ?????? ??????)
    */
    @Test
    void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /*
     *  ????????? ?????? ???????????????, ??? ????????? teamA??? ?????? ??????, ????????? ?????? ??????.
     *
     * */
    @Test
    void join_on_filtering() {

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))//left ????????????????????? ????????? ?????? ??????. inner??? ?????? where?????? ?????? teamA??? ??????????????? ???????????? ??? ??? ??????.
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }


        List<Tuple> result2 = queryFactory
                .select(member, team)
                .from(member)
                .join(member.team, team)//inner join?????? ??????, on??? ????????? where??? ???????????? where??? ??????.
                //.on(team.name.eq("teamA"))
                .where(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result2) {
            System.out.println("tuple = " + tuple);
        }
    }

    /*
     * ??????????????? ?????? ????????? ????????????.
     * ????????? ????????? ??? ????????? ?????? ?????? ????????????.
     *
     * */
    @Test
    void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)//????????? ???????????? ??????????????? ??????.
                .on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void notFetchJoin() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());//???????????? ??????????????? ???????????? ?????????.
        //team??? ????????? lazy???????????? ??????????????? ???????????? ????????? ?????? false??? ??????.
        assertThat(loaded).as("???????????? ?????????").isFalse();
    }

    @Test
    void fetchJoin() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());//???????????? ??????????????? ???????????? ?????????.

        assertThat(loaded).as("???????????? ??????").isTrue();
    }

    /*
     * ????????? ?????? ?????? ?????? ??????.(???????????? ??????)
     * */
    @Test
    void subQuery() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result)
                .extracting("age")
                .containsExactly(40);
    }

    /*
     * ????????? ??????????????? ??????.
     * */
    @Test
    void subQuery2() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result)
                .extracting("age")
                .containsExactly(30, 40);

        List<Member> result2 = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result2)
                .extracting("age")
                .containsExactly(30, 40);
    }

    @Test
    void selectSubQuery() {

        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();
    }

    @Test
    void basicCase() {

        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("??????")
                        .when(20).then("?????????")
                        .otherwise("??????")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void comlexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20")
                        .when(member.age.between(21, 30)).then("21~30")
                        .otherwise("??????"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void constant() {
        List<Tuple> result = queryFactory.select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void concat() {
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void tupleProjection() {
        List<Tuple> tuples = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : tuples) {
            String username = tuple.get(member.username);
            System.out.println("username = " + username);
            Integer age = tuple.get(member.age);
            System.out.println("age = " + age);
        }
    }

    @Test
    void findDtoByJPQL() {
        List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class).getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    @Test
    void findDtoByQuerydsl() {
        List<MemberDto> fetch = queryFactory.select(Projections.bean(MemberDto.class,
                member.username,
                member.age))
                .from(member)
                .fetch();

        List<MemberDto> fetch2 = queryFactory.select(Projections.fields(MemberDto.class,
                member.username,
                member.age))
                .from(member)
                .fetch();

        List<MemberDto> fetch3 = queryFactory.select(Projections.constructor(MemberDto.class,
                member.username,
                member.age))
                .from(member)
                .fetch();

        List<UserDto> fetch4 = queryFactory.select(Projections.bean(UserDto.class,
                member.username.as("name"),
                member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : fetch4) {
            System.out.println("userDto = " + userDto);
        }

        QMember memberSub = new QMember("memberSub");

        List<UserDto> fetch5 = queryFactory
                .select(
                        Projections.bean(UserDto.class,
                                member.username.as("name"),
                                ExpressionUtils.as(
                                        JPAExpressions
                                                .select(memberSub.age.max())
                                                .from(memberSub), "age")
                        )
                )
                .from(member)
                .fetch();
    }

    @Test
    void findDtoByQueryProjection() {
        List<MemberDto> fetch = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void dynamicQuery_BooleanBulider() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> members = searchMember1(usernameParam, ageParam);
        assertThat(members.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameParam, Integer ageParam) {

        BooleanBuilder builder = new BooleanBuilder();

        if(usernameParam != null){
            builder.and(member.username.eq(usernameParam));
        }

        if(ageParam != null){
            builder.and(member.age.eq(ageParam));
        }
        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();

    }

    @Test
    void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> members = searchMember2(usernameParam, ageParam);
        assertThat(members.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameParam, Integer ageParam) {
        return queryFactory
                .selectFrom(member)
                .from(member)
                //.where(usernameEq(usernameParam),ageEq(ageParam))
                .where(allEq(usernameParam, ageParam))
                .fetch();
    }

    private BooleanExpression allEq(String usernameParam, Integer ageParam) {
        return usernameEq(usernameParam).and(ageEq(ageParam));
    }

    private BooleanExpression ageEq(Integer ageParam) {
//        if(ageParam == null){
//            return null;
//        }
//        return member.age.eq(ageParam);
        return ageParam !=null ? member.age.eq(ageParam) : null;
    }

    private BooleanExpression usernameEq(String usernameParam) {
//        if(usernameParam == null){
//            return null;
//        }
//        return member.username.eq(usernameParam);
        return usernameParam !=null ? member.username.eq(usernameParam) : null;
    }

    @Test
    void bulkUpate() {
        queryFactory
                .update(member)
                .set(member.username,"?????????")
                .where(member.age.lt(28))
                .execute();
    }

    @Test
    void bulkAdd() {
        queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    @Test
    void bulkDelete() {
        queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    @Test
    void sqlFunction() {
        List<String> fetch = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace',{0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }
}
