package hello.itemservice;

import hello.itemservice.config.*;
import hello.itemservice.repository.ItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;


//@Import(MemoryConfig.class)
// NOTE : JdbcTemplate 사용
//@Import(JdbcTemplateV1Config.class)
//@Import(JdbcTemplateV2Config.class)
//@Import(JdbcTemplateV3Config.class)
@Import(MyBatisConfig.class)
@Slf4j
@SpringBootApplication(scanBasePackages = "hello.itemservice.web")
public class ItemServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ItemServiceApplication.class, args);
	}

	// NOTE : 사용 방법
	//   @Profile은 spring.profiles.active 속성을 읽어서 프로필로 사용한다.
	//	 @Profile("local")의 경우, application.properties 내부에 spring.profiles.active의 값이 local인 경우만 Bean으로 등록

	// NOTE : @Profile 사용 이유
	//	 		- testDataInit 메소드를 통해 테스트용 더미 데이터를 가져와 local환경에서 실행을 하여 테스트를 할 수 있는 편리함도 있지만
	//			  테스트 코드를 작성하여 실행을 했을 때 testDataInit 메소드의 더미 데이터가 섞여 들어와 테스트 데이터가 오염됩니다.
	//			  ex) 테스트 코드에서 save()를 통해 데이터 하나를 저장하고 카운트를 했으나 tesDataInit의 2개의 데이터로 인해 3개가 카운트 되는 문제 발생
	//			- main 내에 위치한 spring.profiles.active=local로, test 내에 위치한 spring.profiles.active=test로 작성을 하여
	//			  테스트 코드에서는 testDataInit 메소드를 실행하지 않고 테스트용 더미 데이터 역시 생성되지 않기 때문에 정확한 테스트가 가능하다.

	@Bean
	@Profile("local")
	public TestDataInit testDataInit(ItemRepository itemRepository) {
		return new TestDataInit(itemRepository);
	}

	// NOTE : 아래의 임베디드 모드를 생성하는 코드를 주석처리하고 test 내의 application.properties의 데이터베이스에 대한 설정도 주석 처리하여
	//		데이터베이스에 대한 별다른 정보가 없다면 스프링 부트는 임베디드 모드로 접근하는 DataSource를 만들어 제공한다.

	// NOTE : 만약 아래처럼 임베디드 모드를 생성하고 접근하는 코드와 test 내의 application.properties에 서버 모드로 접근하는 코드가 있다면?
	//			- 임베디드 모드로 실행될 지 아니면 서버 모드로 실행 될 지 확인해보니 임베디드 모드로 실행된다.
	//			- 우선순위의 기준이 무엇인지는 아직 모르겠으나 아래의 dataSource() 메소드가 applications.properties보다 높은 것 같다.

//	@Bean
//	@Profile("test")
//	public DataSource dataSource() {
//		log.info("메모리 데이터 베이스 초기화");
//		DriverManagerDataSource dataSource = new DriverManagerDataSource();
//		dataSource.setDriverClassName("org.h2.Driver");
//		// NOTE : "jdbc:h2:mem:db" 처럼 작성 시 임베디드 모드(메모리 모드)로 동작하는 H2 데이터베이스를 사용 할 수 있다.
//		dataSource.setUrl("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1");
//		dataSource.setUsername("sa");
//		dataSource.setPassword("");
//		return dataSource;
//	}

}
