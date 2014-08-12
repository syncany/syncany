# Maintainer: Armin Fisslthaler <armin@fisslthaler.net>
pkgname=syncany-git
pkgver=0.1.8.alpha.g95a5172
pkgrel=1
pkgdesc="Cloud storage and filesharing application with a focus on security and abstraction of storage."
arch=(any)
url=https://www.syncany.org/
license=(GPL3)
depends=('java-runtime>=7')
makedepends=(git)
source=("${pkgname}"::'git+http://github.com/syncany/syncany')
md5sums=('SKIP')

pkgver(){
    cd "$srcdir/$pkgname"
    echo $(grep 'applicationVersion =' build.gradle | cut -d'"' -f2 | sed 's/-/./g').g$(git rev-parse --short HEAD)
}

build(){
    cd "$srcdir/$pkgname"
    ./gradlew installApp
}

package(){
    install -Dm755 "$srcdir/$pkgname/gradle/arch/syncany/syncany" "${pkgdir}/usr/bin/syncany"

    cd "$srcdir/$pkgname/build/install/syncany/lib"
    for jar in *; do
        install -Dm644 "$jar" "${pkgdir}/usr/share/java/syncany/$jar"
    done
}
