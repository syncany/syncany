# Maintainer: Pim Otte <otte dot pim at gmail dot com>
pkgname=syncany
pkgver=0.4.7_alpha
_realver=0.4.7-alpha
pkgrel=2
pkgdesc="Cloud storage and filesharing application with a focus on security and abstraction of storage."
arch=(any)
url="http://www.syncany.org/"
license=('GPL3')
depends=('java-environment>=7', 'sh')
optdepends=('bash-completion: auto completion in bash')
source=("http://syncany.org/dist/$pkgname-${_realver}.tar.gz"
        )
sha256sums=('14d822b34e0a6363fa580104c7fc0d58571b5adb766aff412b12e9f340ce3477')

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
