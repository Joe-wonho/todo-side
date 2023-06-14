package server.blog.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import server.blog.auth.jwt.JwtTokenizer;
import server.blog.auth.utils.UserAuthorityUtils;
import server.blog.awsS3.StorageService;
import server.blog.exception.BusinessLogicException;
import server.blog.exception.ExceptionCode;
import server.blog.user.entity.Users;
import server.blog.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Transactional
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAuthorityUtils authorityUtils;
    private final StorageService storageService;
    private final JwtTokenizer jwtTokenizer;

    public UserService(UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         UserAuthorityUtils authorityUtils,
                         StorageService storageService,
                         JwtTokenizer jwtTokenizer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorityUtils = authorityUtils;
        this.storageService = storageService;
        this.jwtTokenizer = jwtTokenizer;
    }

    /*
    <회원 등록>
    1. 중복 이메일 검증
    2. 중복 닉네임 검증
    3. 패스워드 암호화
    4. Role -> db에 저장
    5. 이미지 -> s3에 저장
    4. 등록
     */
    public Users createUser(Users users, MultipartFile file) throws Exception {


        // 중복 이메일 검증
        verifyExistsEmail(users.getEmail());

        // 중복 닉네임 검증
        verifyExistsNickname(users.getNickname());

        // 패스워드 암호화
        String encryptedPassword = passwordEncoder.encode(users.getPassword());
        users.setPassword(encryptedPassword);

        // Role -> db에 저장
        List<String> roles = authorityUtils.createRoles(users.getEmail());
        users.setRoles(roles);

        String imageUrl = storageService.uploadFile(file);

        users.setProfile(imageUrl);

        return userRepository.save(users);
    }


    /*
      <회원 정보 수정>
      회원 정보는 닉네임, 이미지 변경 가능
     */
    public Users updateUser(Users users, MultipartFile file) throws IOException{


        if (file != null) {

            // 프로필 수정
            String imageUrl = storageService.uploadFile(file, users);
            users.setProfile(imageUrl);
        }

        // 저장
        return userRepository.save(users);
    }

        /*
       <회원 정보 삭제>
       1. 회원 검증(존재O or 존재X)
       2. 삭제
        */
    public void deleteUser(long userId) {
            // 회원 검증(존재O or 존재X)
        Users findUser = checkUser(userId);

        userRepository.delete(findUser);
    }



    private void verifyExistsEmail(String email) throws Exception {
        Optional<Users> optionalMember = userRepository.findByEmail(email);
        if (optionalMember.isPresent()) {
            throw new BusinessLogicException(ExceptionCode.EMAIL_EXIST);
        }
    }


    // 회원 검증 메서드
    private Users checkUser(long userId) {
        return userRepository.findById(userId).orElseThrow(() ->
                new BusinessLogicException(ExceptionCode.USER_NOT_FOUND));
    }


    // 중복 닉네임 검증 메서드
    private void verifyExistsNickname(String nickname) {
        Optional<Users> optionalMember = userRepository.findByNickname(nickname);
        if (optionalMember.isPresent()) {
            throw new BusinessLogicException(ExceptionCode.NICKNAME_EXISTS);
        }
    }
}
