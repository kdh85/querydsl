package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Hello;
import study.querydsl.entity.QHello;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
//@Commit
class QuerydslApplicationTests {

	@Autowired
	EntityManager em;

	@Test
	void contextLoads() {
		Hello hello = new Hello();
		em.persist(hello);
		//Q클래스를 생성하기 위해 Gradle -> 프로젝트 명-> Task -> other -> complieQuerydsld 더블클릭으로 컴파일 실행 필요.
		JPAQueryFactory queryFactory = new JPAQueryFactory(em);
		QHello qHello = QHello.hello;

		Hello hello1 = queryFactory.selectFrom(qHello)
				.fetchOne();

		assertThat(hello1).isEqualTo(hello);
		assertThat(hello1.getId()).isEqualTo(hello.getId());
	}

}
