#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <vector>

using namespace std;
using namespace cv;

extern "C" {
  JNIEXPORT void JNICALL Java_com_lego_minddroid_SampleView_FindLight(JNIEnv* env, jobject thiz, jint width, jint height, jbyteArray yuv, jintArray bgra, jdoubleArray array)
{
    jbyte* _yuv  = env->GetByteArrayElements(yuv, 0);
    jint*  _bgra = env->GetIntArrayElements(bgra, 0);
    jdouble*  _array = env->GetDoubleArrayElements(array, 0);

    Mat myuv(height + height/2, width, CV_8UC1, (unsigned char *)_yuv);
    Mat mbgra(height, width, CV_8UC4, (unsigned char *)_bgra);

    //Please make attention about BGRA byte order
    //ARGB stored in java as int array becomes BGRA at native level
    cvtColor(myuv, mbgra, CV_YUV420sp2BGR, 4);

    Mat mhsv = Mat(mbgra.rows,mbgra.cols,CV_8UC4);
    cvtColor(mbgra, mhsv, CV_BGR2HSV, 4);

    vector<Mat> planes;
    Mat mdetect = Mat(mbgra.rows,mbgra.cols,CV_8UC1);

    Scalar lightLower = Scalar(0, 0, 220);
    Scalar lightUpper = Scalar(255, 10, 255);

    inRange(mhsv, lightLower, lightUpper, mdetect);

    cvtColor(mdetect, mbgra, CV_GRAY2BGRA, 4);

    Moments mm = moments(mdetect,true);
    _array[0] = mm.m00;
    _array[1] = mm.m10/mm.m00;
    _array[2] = mm.m01/mm.m00;

    mhsv.release();
    mdetect.release();

    env->ReleaseIntArrayElements(bgra, _bgra, 0);
    env->ReleaseByteArrayElements(yuv, _yuv, 0);
    env->ReleaseDoubleArrayElements(array, _array, 0);
}

}
