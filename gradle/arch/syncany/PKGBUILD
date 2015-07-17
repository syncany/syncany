# Maintainer: Pim Otte <otte dot pim at gmail dot com>
pkgname=syncany
pkgver=0.4.6_alpha
_realver=0.4.6-alpha
pkgrel=1
pkgdesc="Cloud storage and filesharing application with a focus on security and abstraction of storage."
arch=(any)
url="http://www.syncany.org/"
license=('GPL3')
depends=('java-runtime>=7' 'bash-completion')
source=("http://syncany.org/dist/$pkgname-${_realver}.tar.gz"
        )
sha256sums=('9aab83cc336b898a48dde5e7799d703ef569255e5c6f24a7182a6983c0846bc8')

package() {
    install -Dm644 "$srcdir/$pkgname-${_realver}/bash/syncany.bash-completion" "${pkgdir}/etc/bash_completion.d/syncany"

	cd "$srcdir/$pkgname-${_realver}/man/man"
    for man in *
    do
        install -Dm644 "$man" "${pkgdir}/usr/share/man/man1/$man"
        install -Dm644 "$man" "${pkgdir}/usr/share/man/man1/${man/sy/syncany}"
    done


    install -Dm755 "$srcdir/$pkgname-${_realver}/bin/syncany" "${pkgdir}/usr/share/java/${pkgname}/bin/syncany"
    cd "$srcdir/$pkgname-${_realver}/lib"

    for jar in *
    do
        install -Dm644 "$jar" "${pkgdir}/usr/share/java/${pkgname}/lib/$jar"
    done

    mkdir -p "${pkgdir}/usr/bin"
    ln -s "/usr/share/java/${pkgname}/bin/syncany" "${pkgdir}/usr/bin/syncany"


    #Optional: symlink sy
    #ln -s  "/usr/share/java/${pkgname}/bin/syncany" "${pkgdir}/usr/bin/sy"
}
