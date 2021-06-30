package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

    private final InitMemberService initMemberService;

    //PostConstruct와 Transactional의 라이프 사이클 문제로 인해 분리해서 구현해야 한다.
    @PostConstruct
    public void init(){
        initMemberService.init();
    }

    @Component
    static class InitMemberService{
        @PersistenceContext
        private EntityManager entityManager;

        @Transactional
        public void init(){
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");
            entityManager.persist(teamA);
            entityManager.persist(teamB);

            for(int i=0; i<100; i++){
                Team selectedTeam = i % 2 == 0 ? teamA : teamB;
                entityManager.persist(new Member("member"+i,i,selectedTeam));
            }
        }
    }
}
