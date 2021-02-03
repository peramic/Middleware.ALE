#!/bin/bash

DEFAULT_PROXY_SERVER=http://localhost:3128

echo
echo "Installing texlive and MS fonts, you MUST have root privileges to continue!"
read -p "Press [Enter] to continue... " answer
echo

read -p "Enter proxy server [$DEFAULT_PROXY_SERVER]: " PROXY_SERVER
PROXY_SERVER=${PROXY_SERVER:-$DEFAULT_PROXY_SERVER}

echo ${http_proxy}
export http_proxy=${PROXY_SERVER}
echo ${PROXY_SERVER}
echo ${http_proxy}

apt-get -y install texlive
apt-get -y install texlive-latex-extra
apt-get -y install ttf-mscorefonts-installer

echo
echo "Was MS font installation successful?"
read -p "Press [Enter] to continue... " answer

# refresh font cache
fc-cache -fv

# copy files for conversions
mkdir /tmp/texfonts
cd /tmp/texfonts
cp /usr/share/texlive/texmf-dist/fonts/enc/ttf2pk/base/T1-WGL4.enc .
cp /usr/share/fonts/truetype/msttcorefonts/Verdana.ttf ./verdana.ttf
cp /usr/share/fonts/truetype/msttcorefonts/Verdana_Bold.ttf ./verdanab.ttf
cp /usr/share/fonts/truetype/msttcorefonts/Verdana_Italic.ttf ./verdanai.ttf
cp /usr/share/fonts/truetype/msttcorefonts/Verdana_Bold_Italic.ttf ./verdanaz.ttf
cp /usr/share/fonts/truetype/msttcorefonts/Courier_New.ttf ./courier.ttf
cp /usr/share/fonts/truetype/msttcorefonts/Courier_New_Bold.ttf ./courierb.ttf
cp /usr/share/fonts/truetype/msttcorefonts/Courier_New_Italic.ttf ./courieri.ttf
cp /usr/share/fonts/truetype/msttcorefonts/Courier_New_Bold_Italic.ttf ./courierz.ttf

# create AFMs
ttf2afm -e T1-WGL4.enc -o ecverdana.afm verdana.ttf
ttf2afm -e T1-WGL4.enc -o ecverdanab.afm verdanab.ttf
ttf2afm -e T1-WGL4.enc -o ecverdanai.afm verdanai.ttf
ttf2afm -e T1-WGL4.enc -o ecverdanaz.afm verdanaz.ttf
ttf2afm -e T1-WGL4.enc -o eccourier..afm courier.ttf
ttf2afm -e T1-WGL4.enc -o eccourierb.afm courierb.ttf
ttf2afm -e T1-WGL4.enc -o eccourieri.afm courieri.ttf
ttf2afm -e T1-WGL4.enc -o eccourierz.afm courierz.ttf

# create TFMs
afm2tfm  ecverdana.afm -T T1-WGL4.enc ecverdana.tfm
afm2tfm  ecverdanab.afm -T T1-WGL4.enc ecverdanab.tfm
afm2tfm  ecverdanai.afm -T T1-WGL4.enc ecverdanai.tfm
afm2tfm  ecverdanaz.afm -T T1-WGL4.enc ecverdanaz.tfm
afm2tfm  eccourier.afm -T T1-WGL4.enc eccourier.tfm
afm2tfm  eccourierb.afm -T T1-WGL4.enc eccourierb.tfm
afm2tfm  eccourieri.afm -T T1-WGL4.enc eccourieri.tfm
afm2tfm  eccourierz.afm -T T1-WGL4.enc eccourierz.tfm

# copy TTFs to target
mkdir /usr/share/texlive/texmf-dist/fonts/truetype
mkdir /usr/share/texlive/texmf-dist/fonts/truetype/ms
cp *.ttf /usr/share/texlive/texmf-dist/fonts/truetype/ms

# copy TFMs to target
mkdir /usr/share/texlive/texmf-dist/fonts/tfm/ms
cp *.tfm /usr/share/texlive/texmf-dist/fonts/tfm/ms

# update tex
mktexlsr

# append to pdftex map
cat <<EOF >> /var/lib/texmf/fonts/map/pdftex/updmap/pdftex.map
ecverdana Verdana " T1Encoding ReEncodeFont " <verdana.ttf T1-WGL4.enc
ecverdanai Verdana-Italic " T1Encoding ReEncodeFont " <verdanai.ttf T1-WGL4.enc
ecverdanab Verdana-Bold " T1Encoding ReEncodeFont " <verdanab.ttf T1-WGL4.enc
ecverdanaz Verdana-BoldItalic " T1Encoding ReEncodeFont " <verdanaz.ttf T1-WGL4.enc
eccourier CourierNewPSMT " T1Encoding ReEncodeFont " <courier.ttf T1-WGL4.enc
eccourierb CourierNewPS-BoldMT " T1Encoding ReEncodeFont " <courierb.ttf T1-WGL4.enc
eccourieri CourierNewPS-ItalicMT " T1Encoding ReEncodeFont " <courieri.ttf T1-WGL4.enc
eccourierz CourierNewPS-BoldItalicMT " T1Encoding ReEncodeFont " <courierz.ttf T1-WGL4.enc
EOF

# create style files
mkdir /usr/share/texlive/texmf-dist/tex/latex/truetype
cat <<EOF > /usr/share/texlive/texmf-dist/tex/latex/truetype/verdana.sty
\DeclareFontFamily{T1}{verdana}{}%
\DeclareFontShape{T1}{verdana}{b}{n}{<->ecverdanab}{}%
\DeclareFontShape{T1}{verdana}{b}{it}{<-> ecverdanaz}{}%
%% bold extended (bx) are simply bold
\DeclareFontShape{T1}{verdana}{bx}{n}{<->ssub * verdana/b/n}{}%
\DeclareFontShape{T1}{verdana}{bx}{it}{<->ssub * verdana/b/it}{}%
\DeclareFontShape{T1}{verdana}{m}{n}{<-> ecverdana}{}%
\DeclareFontShape{T1}{verdana}{m}{it}{<-> ecverdanai}{}%
\usepackage[T1]{fontenc}%
\renewcommand{\rmdefault}{verdana}%
\renewcommand{\sfdefault}{verdana}%
EOF

cat <<EOF > /usr/share/texlive/texmf-dist/tex/latex/truetype/courier.sty
\DeclareFontFamily{T1}{courier}{}%
\DeclareFontShape{T1}{courier}{b}{n}{<->eccourierb}{}%
\DeclareFontShape{T1}{courier}{b}{it}{<-> eccourierz}{}%
%% bold extended (bx) are simply bold
\DeclareFontShape{T1}{courier}{bx}{n}{<->ssub * courier/b/n}{}%
\DeclareFontShape{T1}{courier}{bx}{it}{<->ssub * courier/b/it}{}%
\DeclareFontShape{T1}{courier}{m}{n}{<-> eccourier}{}%
\DeclareFontShape{T1}{courier}{m}{it}{<-> eccourieri}{}%
\usepackage[T1]{fontenc}%
\renewcommand{\rmdefault}{courier}%
\renewcommand{\sfdefault}{courier}%
EOF

# update tex one last time
mktexlsr

echo
echo "Successfully installed texlive and MS fonts."
echo
