@GrabResolver(name='artifactory', root='http://artifactory-oss.example.com/artifactory/my-repo-libs-release-local/', m2Compatible=true)
@Grab('org.apache.commons:commons-math3:3.4.1')
import org.apache.commons.math3.primes.Primes

def isPrime(int count) {
	if (!Primes.isPrime(count)) {
		error "${count} was not prime"
	} else {
		echo "${count} is a prime"
	}
}