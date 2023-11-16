#Ejemplo de un clasificador de texto usando una red LSTM con embeddings
#Esta red codifica la semántica de las palabras en función a su contexto y posicion
#El encoder del transformer usa el mecanismo de atención para identificar elementos de la frase que le permitan
#aprender features internas que utiliza para generar la clasificación deseada
import os, numpy as np, pandas as pd
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
from keras.utils import set_random_seed
from keras.models import Sequential, load_model
from keras.optimizers import Adam
from keras.layers import Dense, GlobalAveragePooling1D
from keras.callbacks import EarlyStopping
from keras_nlp.layers import TransformerEncoder, TokenAndPositionEmbedding
from clasificadorLecturaYProcesamientoDatos import lecturaDatosEntrenamientoYTestClasificador, visualizaSerieDatos, guardarSerieDatos

from sklearn.metrics import confusion_matrix
import sys

import leerZaguan


#Definición del modelo usado, embeddings posicionales, el encoder de un transformer,
# un pooling para aplanar la salida del transformer, una densa para procesar el resultado del LSTM
#y una final para clasificar en las categorias deseadas
def createModel(tamVoc,tamFrase,tamEmbd, nClasses):
    model = Sequential()
    model.add(TokenAndPositionEmbedding(tamVoc, tamFrase, tamEmbd))
    model.add(TransformerEncoder(64, num_heads=3 ))
    model.add(TransformerEncoder(32, num_heads=3 ))
    model.add(TransformerEncoder(32, num_heads=5 ))
    model.add(TransformerEncoder(16, num_heads=5 ))

    model.add(GlobalAveragePooling1D())
    model.add(Dense(64, activation='relu'))
    model.add(Dense(32, activation='relu'))

    model.add(Dense(nClasses, activation='softmax'))
    model.compile(loss='CategoricalCrossentropy', optimizer=Adam(1e-4), metrics=['accuracy'])

    return model

# Cargamos los datos y el modelo. Luego lo entrenamos y evaluamos
# El uso de embeddings posicionales y el mecanismo de atención del trasnformer producen resultados razonables
# Puedes cambiar el valor de verbose a 1 si quiere ver el proceso de entrenamiento
if __name__ == '__main__':

    # args: -dir <path> -output <path>
    # -dir: path to the directory containing the data
    # -output: path to the output file

    if len(sys.argv) != 5 or sys.argv[1] != "-dir" or sys.argv[3] != "-output":
        print("Usage: python clasificadorTextoTransformer.py -dir <path> -output <path>")
        exit(1)

    zaguanDir = sys.argv[2]
    output = sys.argv[4]

    leerZaguan.leerZaguan(zaguanDir)

    set_random_seed(0)
    X_entren, y_entren, X_test, y_test, tamVoc, nClasses = lecturaDatosEntrenamientoYTestClasificador()
    model=createModel(tamVoc,len(X_entren[0]),  50, nClasses)

    early_stopping = EarlyStopping(monitor='loss', patience=3, restore_best_weights=True)
    history = model.fit(X_entren, y_entren, epochs=20, validation_steps=10, batch_size=64, callbacks=[early_stopping])

    #La versión de la libreria keras_nlp con el modelo de transformer es experimental y
    #no permite guardar el modelo entrenado

    # Ejemplo de evaluación y clasificación
    scores = model.evaluate(X_test, y_test, verbose=0)
    precision = scores[1] * 100

    # Guardar precision en precision.txt
    f = open("precision.txt", "w")
    f.write(str(precision))
    f.close()

    # visualizamos la evolución del error de entrenamiento
    guardarSerieDatos(history.history['loss'], 'Epoch', 'Error', "error.jpg")

    # visualizamos la estructura del modelo usado
    model.summary()

    #Ejemplo de matriz de confusión
    y_prediction = model.predict(X_test)
    y_prediction = np.argmax(y_prediction, axis=1)
    y_test = np.argmax(y_test, axis=1)

    #Create confusion matrix and normalizes it over predicted (columns)
    result = confusion_matrix(y_test, y_prediction, normalize='pred')

    matrix_size = len(result)


    # Write matrix to txt file without format, one row per line
    f = open("confusion.txt", "w")

    for i in range(matrix_size):
        for j in range(matrix_size):
            f.write(str(result[i][j]) + " ")
        f.write("\n")

    f.close()
